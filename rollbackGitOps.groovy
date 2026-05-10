/**
 * Rollback a GitOps environment by writing the previous image tag as a new commit.
 * Then syncs Argo CD via waitForArgoCDSync and verifies the cluster via verifyK8sDeploymentImageTag.
 *
 * Required args:
 * - environment         — 'staging' or 'prod' (for logging and commit message)
 * - valuesFile          — relative path to values.yaml (e.g. 'environments/podinfo/prod/values.yaml')
 * - previousTag         — image tag to restore (captured before the deploy commit)
 * - gitopsRepo          — HTTPS URL of the gitops repo
 * - gitCredentialsId    — Jenkins credentials ID for Git (username/password or GitHub App)
 * - buildNumber         — Jenkins BUILD_NUMBER for the commit message
 * - argocdApp           — Argo CD child app name (e.g. 'podinfo-prod')
 * - argocdMainApp       — Argo CD parent app name (e.g. 'main-app')
 * - argocdServer        — Argo CD server address
 * - argocdCredentialId  — Jenkins secret id for Argo CD token (e.g. 'argocd-token')
 * - deploymentName      — Kubernetes Deployment name (e.g. 'podinfo')
 * - namespace           — Kubernetes namespace (e.g. 'prod')
 * - imageRepository     — Docker image repository (e.g. 'devuser103/podinfo')
 */
def call(Map args) {
    ['environment', 'valuesFile', 'previousTag', 'gitopsRepo', 'gitCredentialsId',
     'buildNumber', 'argocdApp', 'argocdMainApp', 'argocdServer', 'argocdCredentialId',
     'deploymentName', 'namespace', 'imageRepository'].each { key ->
        if (!args.containsKey(key) || !args[key]?.toString()?.trim()) {
            error("rollbackGitOps: missing required parameter '${key}'")
        }
    }

    def environment          = args.environment.toString().trim()
    def valuesFile           = args.valuesFile.toString().trim()
    def previousTag          = args.previousTag.toString().trim()
    def gitopsRepo           = args.gitopsRepo.toString().trim()
    def gitCredentialsId     = args.gitCredentialsId.toString().trim()
    def buildNumber          = args.buildNumber.toString().trim()
    def argocdApp            = args.argocdApp.toString().trim()
    def argocdMainApp        = args.argocdMainApp.toString().trim()
    def argocdServer         = args.argocdServer.toString().trim()
    def argocdCredentialId   = args.argocdCredentialId.toString().trim()
    def deploymentName       = args.deploymentName.toString().trim()
    def namespace            = args.namespace.toString().trim()
    def imageRepository      = args.imageRepository.toString().trim()

    def dirName    = "gitops-rollback-${environment}-${buildNumber}"
    def valuesYaml = "image:\n  tag: ${previousTag}\n"

    echo "🔄 rollbackGitOps: restoring ${environment} to tag '${previousTag}'"

    // ── 1. Write previous tag as a new commit ─────────────────────
    withCredentials([
        usernamePassword(
            credentialsId: gitCredentialsId,
            usernameVariable: 'GH_USER',
            passwordVariable: 'GH_TOKEN'
        )
    ]) {
        def repoPath = gitopsRepo.replace('https://', '')
        def repoUrl = "https://${env.GH_USER}:${env.GH_TOKEN}@${repoPath}"

        dir(dirName) {
            sh """
                set -eu
                git clone --depth 1 --branch main "${repoUrl}" .
                git remote set-url origin "${repoUrl}"
                git config user.email "jenkins@ci.local"
                git config user.name  "Jenkins CI"
            """
            writeFile(file: valuesFile, text: valuesYaml)
            sh """
                set -eu
                git add "${valuesFile}"
                git commit -m "ci(jenkins-${buildNumber}): rollback ${environment} to ${previousTag} [skip ci]"
                git push origin main
            """
        }
    }

    echo "✅ rollbackGitOps: git updated - ${environment} set back to '${previousTag}'"

    // ── 2. Sync Argo CD and wait for healthy ───────────────────────────────
    waitForArgoCDSync(
        parentAppName: argocdMainApp,
        appName: argocdApp,
        argocdServer: argocdServer,
        credentialsId: argocdCredentialId
    )

    // ── 3. Verify cluster is running the rolled-back image ────────────────
    verifyK8sDeploymentImageTag(
        deploymentName: deploymentName,
        namespace: namespace,
        expectedImage: "${imageRepository}:${previousTag}"
    )

    echo "✅ rollbackGitOps: ${environment} confirmed healthy at '${previousTag}'"
}

// SHARED LIBRARY: GitOps Utilities
//
// WHY THIS IS IN THE SHARED LIBRARY:
// Updating a Helm values file and pushing to a GitOps repo
//
// HOW TO USE IN A JENKINSFILE:
//   @Library('podinfo-shared-lib') _
//   gitopsUtils.updateHelmImageTag(
//       gitopsRepo: 'git@github.com:yourorg/gitops-repo.git',
//       valuesFilePath: 'apps/podinfo/<env>/values.yaml',
//       imageTag: 'abc1234',
//       appVersion: 'v0.2.0',
//       gitCredentialsId: 'github-gitops-key',
//       gitUserEmail: 'jenkins@yourorg.com',
//       gitUserName: 'Jenkins CI'
//   )

/**
 * Clone the GitOps repo, update the image tag in the Helm values file,
 * commit the change, and push back to the repo.
 * ArgoCD watches this repo and will auto-sync the new image to the cluster.
 *
 * @param args Map of parameters (see usage above)
 */
def updateHelmImageTag(Map args) {
    // Validate required parameters — fail early with a clear message
    ['gitopsRepo', 'valuesFilePath', 'imageTag', 'appVersion', 'gitCredentialsId'].each { key ->
        if (!args[key]) {
            error("gitopsUtils.updateHelmImageTag: missing required parameter '${key}'")
        }
    }

    def gitopsRepo      = args.gitopsRepo
    def valuesFilePath  = args.valuesFilePath
    def imageTag        = args.imageTag
    def appVersion      = args.appVersion
    def credentialsId   = args.gitCredentialsId
    def gitEmail        = args.gitUserEmail ?: 'jenkins@ci.local'
    def gitUserName     = args.gitUserName  ?: 'Jenkins CI'
    def targetBranch    = args.branch       ?: 'main'

    echo "GitOps update: ${valuesFilePath} → image.tag=${imageTag}"

    // Use Jenkins SSH credentials to clone the private gitops repo
    sshagent(credentials: [credentialsId]) {
        sh """
            set -e

            # Clone the gitops repo into a subdirectory to avoid polluting
            # the main workspace (which contains the app source)
            rm -rf gitops-workspace
            git clone --depth=1 --branch ${targetBranch} ${gitopsRepo} gitops-workspace
            cd gitops-workspace

            # Configure git identity for the commit
            git config user.email "${gitEmail}"
            git config user.name "${gitUserName}"

            # Update the image tag in the Helm values file using sed.
            # We target the specific line to avoid accidentally modifying
            # other tag fields in the YAML.
            # Expected values.yaml format:
            #   image:
            #     repository: 123456789.dkr.ecr.us-east-1.amazonaws.com/podinfo
            #     tag: "abc1234"     ← this line gets updated
            sed -i 's|^  tag:.*|  tag: "${imageTag}"|' ${valuesFilePath}

            # Also update appVersion if the values file tracks it
            sed -i 's|^APP_VERSION:.*|APP_VERSION: "${appVersion}"|' ${valuesFilePath} || true

            # Verify the change was made — fail if sed had no effect
            grep "tag: \\"${imageTag}\\"" ${valuesFilePath} || {
                echo "ERROR: sed did not update the image tag. Check the values file format."
                cat ${valuesFilePath}
                exit 1
            }

            # Stage and commit
            git add ${valuesFilePath}

            # Check if there's actually something to commit
            # (idempotent: if the tag is already set, don't create an empty commit)
            if git diff --cached --quiet; then
                echo "No changes to commit — image tag ${imageTag} is already set in ${valuesFilePath}"
            else
                git commit -m "chore(deploy): update podinfo image tag to ${imageTag}

App version: ${appVersion}
Triggered by Jenkins build: ${env.BUILD_URL}
Git SHA: ${imageTag}"

                git push origin ${targetBranch}
                echo "GitOps repo updated successfully — ArgoCD will pick up the change"
            fi
        """
    }
}
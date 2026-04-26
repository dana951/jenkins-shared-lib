/**
 * Update GitOps values for an environment and wait for ArgoCD rollout.
 *
 * Required args:
 * - environment: target environment name (dev|qa|staging|prod)
 * - valuesFilePath: path in gitops-manifests repo to values.yaml
 * - imageTag: image tag to deploy
 * - gitopsRepo: ssh url for gitops repository
 * - gitCredentialsId: Jenkins SSH credential id
 *
 * Optional args:
 * - appName: base app name used for ArgoCD app naming (default: podinfo)
 * - appVersion: version value written to values (default: imageTag)
 * - gitUserEmail: commit author email (default: jenkins@ci.local)
 * - gitUserName: commit author name (default: Jenkins CI)
 */
def call(Map args) {
    ['environment', 'valuesFilePath', 'imageTag', 'gitopsRepo', 'gitCredentialsId'].each { key ->
        if (!args[key]) {
            error("deployToEnvironment: missing required parameter '${key}'")
        }
    }

    def environmentName = args.environment
    def valuesFilePath = args.valuesFilePath
    def imageTag = args.imageTag
    def appName = args.appName ?: 'podinfo'
    def appVersion = args.appVersion ?: imageTag
    def gitUserEmail = args.gitUserEmail ?: 'jenkins@ci.local'
    def gitUserName = args.gitUserName ?: 'Jenkins CI'

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Deploying ${imageTag} → ${environmentName.toUpperCase()}"
    echo "  Values file: ${valuesFilePath}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    gitopsUtils.updateHelmImageTag(
        gitopsRepo: args.gitopsRepo,
        valuesFilePath: valuesFilePath,
        imageTag: imageTag,
        appVersion: appVersion,
        gitCredentialsId: args.gitCredentialsId,
        gitUserEmail: gitUserEmail,
        gitUserName: gitUserName
    )

    waitForArgoCDSync(appName: "${appName}-${environmentName}")
}

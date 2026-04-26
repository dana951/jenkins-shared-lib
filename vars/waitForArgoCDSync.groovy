/**
 * Trigger an ArgoCD sync and wait for Healthy + Synced status.
 *
 * Required args:
 * - appName: ArgoCD application name (e.g. podinfo-dev)
 *
 * Optional args:
 * - argocdServer: ArgoCD server DNS (default: in-cluster service)
 * - timeoutSeconds: wait timeout in seconds (default: 300)
 * - credentialsId: Jenkins string credential id for ArgoCD token
 */
def call(Map args) {
    if (!args.appName) {
        error("waitForArgoCDSync: missing required parameter 'appName'")
    }

    def appName = args.appName
    def argocdServer = args.argocdServer ?: 'argocd-server.argocd.svc.cluster.local'
    def timeoutSeconds = args.timeoutSeconds ?: 300
    def credentialsId = args.credentialsId ?: 'argocd-auth-token'

    echo "Waiting for ArgoCD sync: ${appName}"

    withCredentials([string(credentialsId: credentialsId, variable: 'ARGOCD_TOKEN')]) {
        sh """
            set -e

            // argocd app sync ${appName} \
            //     --server ${argocdServer} \
            //     --auth-token \$ARGOCD_TOKEN \
            //     --grpc-web \
            //     --async

            argocd app wait ${appName} \
                --server ${argocdServer} \
                --auth-token \$ARGOCD_TOKEN \
                --grpc-web \
                --health \
                --sync \
                --timeout ${timeoutSeconds}
        """
    }

    echo "✅ ${appName} is Healthy and Synced"
}

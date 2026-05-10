/**
 * Required:
 * - parentAppName (e.g. env.ARGOCD_MAIN_APP_NAME)
 * - appName       (e.g. env.ARGOCD_APP_NAME)
 * - argocdServer  (e.g. env.ARGOCD_SERVER)
 * - credentialsId (Jenkins secret id for the Argo CD token, e.g. 'argocd-token')
 *
 * Optional (defaults):
 * - argocdAppGetTimeoutSeconds   — timeout for waitUntil / argocd app get polling (default: 300 seconds)
 * - insecure                     — when true, passes --insecure to argocd (default: false)
 * - argocdAppWaitTimeoutSeconds  — argocd app wait --timeout (default: 300 seconds)
 */
def call(Map args) {
    ['parentAppName', 'appName', 'argocdServer', 'credentialsId'].each { key ->
        if (!args.containsKey(key)) {
            error("waitForArgoCDSync: missing required parameter '${key}'")
        }
    }

    if (args.containsKey('insecure') && !(args.insecure instanceof Boolean)) {
        error('waitForArgoCDSync: insecure must be a Boolean when provided')
    }

    def parentAppName = args.parentAppName.toString().trim()
    def appName = args.appName.toString().trim()
    def argocdServer = args.argocdServer.toString().trim()
    def credentialsId = args.credentialsId.toString().trim()
    def insecure = args.containsKey('insecure') ? args.insecure : false
    def argocdAppGetTimeoutSeconds = (args.argocdAppGetTimeoutSeconds != null)
        ? (args.argocdAppGetTimeoutSeconds as Integer)
        : 300
    def argocdAppWaitTimeoutSeconds = (args.argocdAppWaitTimeoutSeconds != null)
        ? (args.argocdAppWaitTimeoutSeconds as Integer)
        : 300

    if (!parentAppName || !appName || !argocdServer || !credentialsId) {
        error('waitForArgoCDSync: parentAppName, appName, argocdServer, and credentialsId must be non-empty')
    }

    def insecureFlag = insecure ? ' --insecure' : ''

    withCredentials([string(credentialsId: credentialsId, variable: 'ARGO_TOKEN')]) {
        echo "Syncing Parent App: ${parentAppName}"
        sh """
            set -eu
            argocd app sync '${parentAppName}' \\
                --auth-token "\$ARGO_TOKEN" \\
                --server '${argocdServer}'${insecureFlag}
        """.stripIndent().trim()

        echo "Waiting for Application '${appName}' to appear..."
        timeout(time: argocdAppGetTimeoutSeconds, unit: 'SECONDS') {
            waitUntil {
                def appExists = sh(
                    script: """
                        set -eu
                        argocd app get '${appName}' \\
                            --auth-token "\$ARGO_TOKEN" \\
                            --server '${argocdServer}'${insecureFlag}
                    """.stripIndent().trim(),
                    returnStatus: true
                )
                if (appExists != 0) {
                    sleep(time: 10, unit: 'SECONDS')
                }
                return (appExists == 0)
            }
        }

        echo "Application '${appName}' created! Waiting for deployment to complete..."
        sh """
            set -eu
            argocd app wait '${appName}' \\
                --auth-token "\$ARGO_TOKEN" \\
                --server '${argocdServer}'${insecureFlag} \\
                --sync \\
                --health \\
                --timeout ${argocdAppWaitTimeoutSeconds}
        """.stripIndent().trim()

        echo "Deployment of ${appName} is successful and healthy."
    }
}

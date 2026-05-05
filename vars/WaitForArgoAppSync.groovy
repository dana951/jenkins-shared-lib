/**
 * Wait until an ArgoCD Application is Healthy.
 *
 * Required args:
 * - appName: ArgoCD Application name
 * - namespace: Namespace where the ArgoCD Application CRD lives
 *
 * Optional args:
 * - expectedImageTag: if set, also wait until deployed images contain this tag
 * - timeoutSeconds: Max wait time in seconds (default: 300)
 * - pollSeconds: Poll interval in seconds (default: 10)
 */
def call(Map args) {
    ['appName', 'namespace'].each { key ->
        if (!args[key]) {
            error("WaitForArgoApp: missing required parameter '${key}'")
        }
    }

    def appName = args.appName
    def namespace = args.namespace
    def expectedImageTag = args.expectedImageTag
    def timeoutSeconds = (args.timeoutSeconds ?: 300) as Integer
    def pollSeconds = (args.pollSeconds ?: 10) as Integer

    def deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L)
    def ready = false

    while (System.currentTimeMillis() < deadline && !ready) {
        def health = sh(
            script: "kubectl get application ${appName} -n ${namespace} -o jsonpath='{.status.health.status}' 2>/dev/null || true",
            returnStdout: true
        ).trim()

        if (expectedImageTag) {
            def deployed = sh(
                script: "kubectl get application ${appName} -n ${namespace} -o jsonpath='{.status.summary.images}' 2>/dev/null || true",
                returnStdout: true
            ).trim()
            echo "ArgoCD ${appName} health.status=${health}"
            echo "Deployed images: ${deployed}"
            if (health == 'Healthy' && deployed.contains(expectedImageTag)) {
                ready = true
            } else if (health == 'Healthy') {
                echo "ArgoCD is Healthy but deployed image does not match ${expectedImageTag}"
                sleep(pollSeconds)
            } else {
                sleep(pollSeconds)
            }
        } else {
            echo "ArgoCD Application ${appName} health.status: '${health}'"
            if (health == 'Healthy') {
                ready = true
            } else {
                sleep(pollSeconds)
            }
        }
    }

    if (!ready) {
        if (expectedImageTag) {
            error("Application ${appName} did not become Healthy with image containing ${expectedImageTag} within ${timeoutSeconds}s")
        }
        error("Application ${appName} did not become Healthy within ${timeoutSeconds}s")
    }

    if (expectedImageTag) {
        echo "✅ ArgoCD Application ${appName} is Healthy with image tag ${expectedImageTag}"
    } else {
        echo "✅ ArgoCD Application ${appName} is Healthy"
    }
}

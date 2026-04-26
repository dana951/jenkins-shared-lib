// SHARED LIBRARY: Smoke Test Utilities
//
// WHY THIS IS IN THE SHARED LIBRARY:
// Post-deployment health checking.
// wait for the app to become ready, then hit known endpoints and validate responses.
//
// HOW TO USE IN A JENKINSFILE:
//   smokeTestUtils.waitForDeployment(appUrl: 'http://podinfo.dev.svc.cluster.local:8080')
//   smokeTestUtils.runSmokeSuite(appUrl: 'http://podinfo.dev.svc.cluster.local:8080', expectedVersion: 'v0.2.0')

/**
 * Wait for the application to become ready by polling /health.
 * Retries with exponential backoff — ArgoCD sync and pod rollout take time.
 *
 * @param appUrl         Base URL of the deployed app (no trailing slash)
 * @param maxRetries     Maximum number of health check attempts (default: 20)
 * @param intervalSecs   Seconds between retries (default: 15)
 */
def waitForDeployment(Map args) {
    def appUrl       = args.appUrl
    def maxRetries   = args.maxRetries   ?: 20
    def intervalSecs = args.intervalSecs ?: 15

    if (!appUrl) error("smokeTestUtils.waitForDeployment: 'appUrl' is required")

    echo "Waiting for app to become healthy at ${appUrl}/health"
    echo "Max retries: ${maxRetries}, interval: ${intervalSecs}s (max wait: ${maxRetries * intervalSecs}s)"

    def attempt = 0
    def ready = false

    while (attempt < maxRetries && !ready) {
        attempt++
        echo "Health check attempt ${attempt}/${maxRetries}..."

        def httpStatus = sh(
            script: "curl -s -o /dev/null -w '%{http_code}' --connect-timeout 5 ${appUrl}/health",
            returnStdout: true
        ).trim()

        if (httpStatus == '200') {
            ready = true
            echo "App is healthy after ${attempt} attempt(s)"
        } else {
            echo "Got HTTP ${httpStatus}, retrying in ${intervalSecs}s..."
            sleep(intervalSecs)
        }
    }

    if (!ready) {
        error("App did not become healthy after ${maxRetries} attempts (${maxRetries * intervalSecs}s). " +
              "Check ArgoCD sync status and pod logs.")
    }
}

/**
 * Run the full smoke test suite against the deployed app.
 * Tests the key endpoints to verify the deployment is functional.
 *
 * @param appUrl          Base URL of the deployed app
 * @param expectedVersion Expected APP_VERSION value from /version endpoint
 */
def runSmokeSuite(Map args) {
    def appUrl          = args.appUrl
    def expectedVersion = args.expectedVersion ?: ''

    if (!appUrl) error("smokeTestUtils.runSmokeSuite: 'appUrl' is required")

    echo "Running smoke test suite against ${appUrl}"

    // We use Python (available in the Jenkins agent) to run pytest smoke tests.
    // This is cleaner than inline shell assertions — we get proper test output,
    // failure messages, and can add more tests without touching the Jenkinsfile.
    sh """
        set -e

        pip install pytest httpx --quiet

        # Run only tests marked @pytest.mark.smoke
        # These tests live in the app-source repo, which Jenkins checks out
        APP_BASE_URL=${appUrl} \
        EXPECTED_VERSION=${expectedVersion} \
        pytest e2e/ -m smoke --tb=short -v
    """
}
/**
 * Checkout source and run pytest E2E tests with environment-driven config.
 *
 * Required args:
 * - environment: env label used by tests (dev|qa|staging|prod)
 * - appUrl: base service URL
 * - expectedVersion: expected app version/tag returned by service
 * - markers: pytest marker expression (e.g. "smoke or e2e")
 */
def call(Map args) {
    ['environment', 'appUrl', 'expectedVersion', 'markers'].each { key ->
        if (!args[key]) {
            error("runE2ETests: missing required parameter '${key}'")
        }
    }

    def environmentName = args.environment
    def appUrl = args.appUrl
    def expectedVersion = args.expectedVersion
    def markers = args.markers

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "E2E Tests → ${environmentName.toUpperCase()}"
    echo "  App URL: ${appUrl}"
    echo "  Markers: ${markers}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    checkout scm

    sh """
        set -e

        pip install -r e2e/requirements.txt --quiet

        APP_BASE_URL=${appUrl} \
        EXPECTED_VERSION=${expectedVersion} \
        APP_ENV=${environmentName} \
        pytest e2e/ \
            -m '${markers}' \
            --tb=short \
            -v
    """

    echo "✅ E2E tests passed on ${environmentName.toUpperCase()}"
}

/**
 * Checkout tests-repo and run pytest with environment-driven config.
 *
 * Required args:
 * - environment: env label used by tests (qa|staging|prod)
 * - appUrl: base service URL
 * - expectedVersion: expected app version returned by service
 * - markers: pytest marker expression (e.g. "e2e" or "smoke")
 *
 * Runs tests based on the provided marker expression.
 */

def call(Map args) {
    // 1. Validation logic
    ['environment', 'appUrl', 'expectedVersion', 'markers'].each { key ->
        if (!args[key]) {
            error("runTests: missing required parameter '${key}'")
        }
    }

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "  Target Env: ${args.environment}"
    echo "  App URL: ${args.appUrl}"
    echo "  Expected Version: ${args.expectedVersion}"
    echo "  Markers: ${args.markers}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    dir('tests-repo') {

        checkout([
            $class: 'GitSCM',
            branches: [[name: '*/main']],
            userRemoteConfigs: [[url: 'https://github.com/dana951/tests-repo.git']],
            extensions: [[
                $class: 'CloneOption',
                shallow: true,
                depth: 1,
                noTags: false
            ]]
        ])

        sh """
            set -eu
            
            # Install dependencies
            uv sync --quiet

            # Run pytest with the specific marker provided in args
            uv run pytest . \
                -m "${args.markers}" \
                --env="${args.environment}" \
                --base-url="${args.appUrl}" \
                --expected-version="${args.expectedVersion}" \
                --tb=short \
                -v
        """
    }

    echo "✅ Tests with markers [${args.markers}] passed!"
}

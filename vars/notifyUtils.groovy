// SHARED LIBRARY: Notification Utilities
//
// WHY THIS IS IN THE SHARED LIBRARY:
// Notification logic (Slack, email, etc.).
// Every pipeline needs to notify on success and failure.
//
// HOW TO USE IN A JENKINSFILE:
//   notifyUtils.deploymentSuccess(appName: 'podinfo', version: 'v0.2.0', environment: 'dev')
//   notifyUtils.deploymentFailure(appName: 'podinfo', stage: 'smoke-test', buildUrl: env.BUILD_URL)

/**
 * Send a Slack deployment success notification.
 * Requires the Jenkins Slack plugin and a SLACK_WEBHOOK_URL credential.
 */
def deploymentSuccess(Map args) {
    def appName     = args.appName     ?: 'unknown-app'
    def version     = args.version     ?: 'unknown'
    def environment = args.environment ?: 'unknown'
    def imageTag    = args.imageTag    ?: 'unknown'
    def buildUrl    = args.buildUrl    ?: env.BUILD_URL

    def message = """
{
  "text": ":white_check_mark: *Deployment Successful*",
  "attachments": [{
    "color": "good",
    "fields": [
      {"title": "App",         "value": "${appName}",     "short": true},
      {"title": "Version",     "value": "${version}",     "short": true},
      {"title": "Environment", "value": "${environment}", "short": true},
      {"title": "Image Tag",   "value": "${imageTag}",    "short": true},
      {"title": "Build",       "value": "<${buildUrl}|View Build>", "short": false}
    ]
  }]
}
"""
    _sendSlack(message)
}

/**
 * Send a Slack deployment failure notification.
 */
def deploymentFailure(Map args) {
    def appName  = args.appName  ?: 'unknown-app'
    def stage    = args.stage    ?: 'unknown-stage'
    def buildUrl = args.buildUrl ?: env.BUILD_URL

    def message = """
{
  "text": ":x: *Deployment Failed*",
  "attachments": [{
    "color": "danger",
    "fields": [
      {"title": "App",          "value": "${appName}", "short": true},
      {"title": "Failed Stage", "value": "${stage}",   "short": true},
      {"title": "Build",        "value": "<${buildUrl}|View Build>", "short": false}
    ]
  }]
}
"""
    _sendSlack(message)
}

// Private helper — not intended for direct use in Jenkinsfiles
def _sendSlack(String payload) {
    withCredentials([string(credentialsId: 'slack-webhook-url', variable: 'SLACK_WEBHOOK')]) {
        sh """
            curl -s -X POST \
                -H 'Content-type: application/json' \
                --data '${payload}' \
                "\$SLACK_WEBHOOK"
        """
    }
}
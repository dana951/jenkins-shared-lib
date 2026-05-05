/**
 * Simple Slack notification utility.
 *
 * Required args:
 * - message: Slack message text
 *
 * Optional args:
 * - channel: Slack channel (default: "#deployments")
 * - color: attachment color (default: "good")
 * - tokenCredentialId: Jenkins Slack token/webhook credential id (default: "slack-webhook-url")
 */
def call(Map args) {
    if (!args.message) {
        error("notifyUtils: missing required parameter 'message'")
    }

    def message = args.message
    def channel = args.channel ?: '#deployments'
    def color = args.color ?: 'good'
    def tokenCredentialId = args.tokenCredentialId ?: 'slack-webhook-url'

    slackSend(
        channel: channel,
        color: color,
        message: message,
        tokenCredentialId: tokenCredentialId
    )
}
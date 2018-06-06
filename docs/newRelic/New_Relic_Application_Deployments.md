# New Relic Application Deployments

## Purpose

New Relic APM's Deployments page lists recent deployments and their
impact on your end user and app server's Apdex scores, response times,
throughput, and errors. Deployments will also show in any charts showing
metrics for an app within the NR UI. This gives engineers more context
as to what might have affected application performance and when.

Herman is set up to send the Bamboo release ID and the Git revision to
the New Relic application deployments REST endpoint after each
successful app deploy for apps that are set up to use the New Relic
agent.

## Herman Output

Herman will show if an application was found and if an application deployment
record was added. The link to the application in New Relic will be shown
if the application was found in NR.

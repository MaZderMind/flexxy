#!/bin/bash
set -euo pipefail

# Jira Cloud
curl -L https://developer.atlassian.com/cloud/jira/platform/swagger-v3.v3.json | jq > jira.original.json
jq '
  # Patch up StringList with actual string
  (.paths[][].parameters[].schema.items | select(."$ref" == "#/components/schemas/StringList")) |= {type: "string"}
' <json/jira.original.json >json/jira.json

# Confluence Cloud
curl -L https://developer.atlassian.com/cloud/confluence/swagger.v3.json | jq > json/confluence.original.json
cp json/confluence.original.json json/confluence.json

# Jira Service-Desk Cloud
curl -L https://developer.atlassian.com/cloud/jira/service-desk/swagger.v3.json | jq > json/jira-service-desk.original.json
jq '
  # Patch up duplicate operationIds
  (.paths["/rest/servicedeskapi/knowledgebase/article"]["get"].operationId = "getAllArticles") |
  (.paths["/rest/servicedeskapi/organization"]["get"].operationId = "getAllOrganizations") |
  (.paths["/rest/servicedeskapi/organization/{organizationId}/property"]["get"].operationId = "getOrganizationPropertiesKeys") |
  (.paths["/rest/servicedeskapi/organization/{organizationId}/property/{propertyKey}"]["get"].operationId = "getOrganizationProperty") |
  (.paths["/rest/servicedeskapi/organization/{organizationId}/property/{propertyKey}"]["put"].operationId = "setOrganizationProperty") |
  (.paths["/rest/servicedeskapi/organization/{organizationId}/property/{propertyKey}"]["delete"].operationId = "deleteOrganizationProperty")
' <json/jira-service-desk.original.json >json/jira-service-desk.json


# Jira Software Cloud
curl -L https://developer.atlassian.com/cloud/jira/software/swagger.v3.json | jq > json/jira-software.original.json
cp json/jira-software.original.json json/jira-software.json

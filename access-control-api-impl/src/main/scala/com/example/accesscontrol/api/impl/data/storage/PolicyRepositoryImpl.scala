package com.example.accesscontrol.api.impl.data.storage

import com.example.accesscontrol.api.impl.domain.PolicyRepository
import play.api.libs.json.{JsValue, Json}

final class PolicyRepositoryImpl extends PolicyRepository {
  override def fetchPolicyCollection: JsValue =
    Json.parse("""
      {"policySets": [
        {
          "_type": "PolicySet",
          "target": {
            "_type": "ObjectTypeTarget",
            "value": "bicycle"
          },
          "combiningAlgorithm": "DenyOverride",
          "policies": [{
            "_type": "Policy",
            "target": {
              "_type": "ActionTypeTarget",
              "value": "ride"
            },
            "combiningAlgorithm": "DenyOverride",
            "rules": [{
              "_type": "Rule",
              "target": {
                "_type": "AttributeTypeTarget",
                "value": "permissionToRideBicycle"
              },
              "positiveEffect": {
                "_type": "PositiveEffect",
                "decision": "Permit"
              },
              "negativeEffect": {
                "_type": "NegativeEffect",
                "decision": "Deny"
              },
              "condition": {
                "_type": "CompareConditionSerializable",
                "operation": "eq",
                "leftOperand": {
                  "_type": "AttributeValue",
                  "id": "permissionToRideBicycle"
                },
                "rightOperand": {
                  "_type": "BoolValue",
                  "value": true
                }
              }
            }]
          },
          {
            "_type": "Policy",
            "target": {
              "_type": "ActionTypeTarget",
              "value": "rent"
            },
            "combiningAlgorithm": "PermitOverride",
            "rules": [{
              "_type": "Rule",
              "target": {
                "_type": "AttributeTypeTarget",
                "value": "permissionToRentBicycle"
              },
              "positiveEffect": {
                "_type": "PositiveEffect",
                "decision": "Permit"
              },
              "negativeEffect": {
                "_type": "NegativeEffect",
                "decision": "Deny"
              },
              "condition": {
                "_type": "CompositeConditionSerializable",
                "predicate": "AND",
                "leftCondition": {
                  "_type": "CompareConditionSerializable",
                  "operation": "eq",
                  "leftOperand": {
                    "_type": "AttributeValue",
                    "id": "permissionToRentBicycle"
                  },
                  "rightOperand": {
                    "_type": "BoolValue",
                    "value": true
                  }
                },
                "rightCondition": {
                  "_type": "CompositeConditionSerializable",
                  "predicate": "OR",
                  "leftCondition": {
                    "_type": "CompareConditionSerializable",
                    "operation": "gte",
                    "leftOperand": {
                      "_type": "AttributeValue",
                      "id": "personAge"
                    },
                    "rightOperand": {
                      "_type": "IntValue",
                      "value": 18
                    }
                  },
                  "rightCondition": {
                    "_type": "CompareConditionSerializable",
                    "operation": "eq",
                    "leftOperand": {
                      "_type": "AttributeValue",
                      "id": "bicycleType"
                    },
                    "rightOperand": {
                      "_type": "StringValue",
                      "value": "tricycle"
                    }
                  }
                }
              }
            }]
          }]
        },
        {
          "_type": "PolicySet",
          "target": {
            "_type": "ObjectTypeTarget",
            "value": "skateboard"
          },
          "combiningAlgorithm": "DenyOverride",
          "policies": [{
            "_type": "Policy",
            "target": {
              "_type": "ActionTypeTarget",
              "value": "ride"
            },
            "combiningAlgorithm": "DenyOverride",
            "rules": [{
              "_type": "Rule",
              "target": {
                "_type": "AttributeTypeTarget",
                "value": "permissionToRideSkateboard"
              },
              "positiveEffect": {
                "_type": "PositiveEffect",
                "decision": "Permit"
              },
              "negativeEffect": {
                "_type": "NegativeEffect",
                "decision": "Deny"
              },
              "condition": {
                "_type": "CompareConditionSerializable",
                "operation": "eq",
                "leftOperand": {
                  "_type": "AttributeValue",
                  "id": "permissionToRideSkateboard"
                },
                "rightOperand": {
                  "_type": "BoolValue",
                  "value": true
                }
              }
            },
            {
              "_type": "Rule",
              "target": {
                "_type": "AttributeTypeTarget",
                "value": "placeType"
              },
              "positiveEffect": {
                "_type": "PositiveEffect",
                "decision": "Deny"
              },
              "negativeEffect": {
                "_type": "NegativeEffect",
                "decision": "Permit"
              },
              "condition": {
                "_type": "CompareConditionSerializable",
                "operation": "eq",
                "leftOperand": {
                  "_type": "AttributeValue",
                  "id": "placeType"
                },
                "rightOperand": {
                  "_type": "StringValue",
                  "value": "office"
                }
              }
            }]
          }]
        },
        {
          "_type": "PolicySet",
          "target": {
            "_type": "ObjectTypeTarget",
            "value": "scooter"
          },
          "combiningAlgorithm": "PermitOverride",
          "policies": [{
            "_type": "Policy",
            "target": {
              "_type": "ActionTypeTarget",
              "value": "ride"
            },
            "combiningAlgorithm": "PermitOverride",
            "rules": [{
              "_type": "Rule",
              "target": {
                "_type": "AttributeTypeTarget",
                "value": "permissionToRideScooter"
              },
              "positiveEffect": {
                "_type": "PositiveEffect",
                "decision": "Permit"
              },
              "negativeEffect": {
                "_type": "NegativeEffect",
                "decision": "Deny"
              },
              "condition": {
                "_type": "CompareConditionSerializable",
                "operation": "eq",
                "leftOperand": {
                  "_type": "AttributeValue",
                  "id": "permissionToRideScooter"
                },
                "rightOperand": {
                  "_type": "BoolValue",
                  "value": true
                }
              }
            },
            {
              "_type": "Rule",
              "target": {
                "_type": "AttributeTypeTarget",
                "value": "profession"
              },
              "positiveEffect": {
                "_type": "PositiveEffect",
                "decision": "Permit"
              },
              "negativeEffect": {
                "_type": "NegativeEffect",
                "decision": "Deny"
              },
              "condition": {
                "_type": "CompareConditionSerializable",
                "operation": "eq",
                "leftOperand": {
                  "_type": "AttributeValue",
                  "id": "profession"
                },
                "rightOperand": {
                  "_type": "StringValue",
                  "value": "courier"
                }
              }
            }]
          }]
        }
      ]}
      """)
}

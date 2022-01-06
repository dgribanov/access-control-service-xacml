package com.example.accesscontrol.admin.ws.rest.api

import play.api.libs.json.{Format, Json}

case class RegisterPolicySetCommand(policySet: PolicySet)

object RegisterPolicySetCommand {
  /**
   * Format for converting register policy set command object to and from JSON.
   * This will be picked up by a Lagom implicit conversion from Play's JSON format to Lagom's message serializer.
   */
  implicit val format: Format[RegisterPolicySetCommand] = Json.format[RegisterPolicySetCommand]
}

case class PolicySet(target: TargetType, combiningAlgorithm: CombiningAlgorithms.Algorithm, policies: Array[Policy])

object PolicySet {
  implicit val format: Format[PolicySet] = Json.format[PolicySet]
}

case class Policy(target: TargetType, combiningAlgorithm: CombiningAlgorithms.Algorithm, rules: Array[Rule])

object Policy {
  implicit val format: Format[Policy] = Json.format[Policy]
}

case class Rule(target: TargetType, condition: Condition, positiveEffect: PositiveEffect, negativeEffect: NegativeEffect)

object Rule {
  implicit val format: Format[Rule] = Json.format[Rule]
}

abstract class TargetType
case class ObjectTypeTarget(value: String) extends TargetType
case class ActionTypeTarget(value: String) extends TargetType
case class AttributeTypeTarget(value: String) extends TargetType

object ObjectTypeTarget {
  implicit val format: Format[ObjectTypeTarget] = Json.format[ObjectTypeTarget]
}

object ActionTypeTarget {
  implicit val format: Format[ActionTypeTarget] = Json.format[ActionTypeTarget]
}

object AttributeTypeTarget {
  implicit val format: Format[AttributeTypeTarget] = Json.format[AttributeTypeTarget]
}

object TargetType {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[TargetType] = Format[TargetType] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "_type").read[String].reads(js)
      valueType.fold(
        _ => JsError("type undefined or incorrect"),
        {
          case "ObjectTypeTarget"    => JsPath.read[ObjectTypeTarget].reads(js)
          case "ActionTypeTarget"    => JsPath.read[ActionTypeTarget].reads(js)
          case "AttributeTypeTarget" => JsPath.read[AttributeTypeTarget].reads(js)
        }
      )
    },
    Writes {
      case target: ObjectTypeTarget =>
        JsObject(
          Seq(
            "_type" -> JsString("ObjectTypeTarget"),
            "id"    -> ObjectTypeTarget.format.writes(target)
          )
        )
      case target: ActionTypeTarget =>
        JsObject(
          Seq(
            "_type" -> JsString("ActionTypeTarget"),
            "value" -> ActionTypeTarget.format.writes(target)
          )
        )
      case target: AttributeTypeTarget =>
        JsObject(
          Seq(
            "_type" -> JsString("AttributeTypeTarget"),
            "value" -> AttributeTypeTarget.format.writes(target)
          )
        )
    }
  )
}

case class CombineRulesAlgorithm(algorithm: String)

object CombineRulesAlgorithm {
  implicit val format: Format[CombineRulesAlgorithm] = Json.format[CombineRulesAlgorithm]
}

sealed trait Condition
case class CompareCondition(operation: String, leftOperand: ExpressionParameterValue, rightOperand: ExpressionParameterValue) extends Condition
case class CompositeCondition(predicate: String, leftCondition: Condition, rightCondition: Condition) extends Condition

object CompareCondition {
  implicit val format: Format[CompareCondition] = Json.format[CompareCondition]
}

object CompositeCondition {
  implicit val format: Format[CompositeCondition] = Json.format[CompositeCondition]
}

object Condition {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[Condition] = Format[Condition] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "_type").read[String].reads(js)
      valueType.fold(
        _ => JsError("type undefined or incorrect"),
        {
          case "CompareCondition"   => JsPath.read[CompareCondition].reads(js)
          case "CompositeCondition" => JsPath.read[CompositeCondition].reads(js)
        }
      )
    },
    Writes {
      case condition: CompareCondition =>
        JsObject(
          Seq(
            "_type"        -> JsString("CompareCondition"),
            "operation"    -> CompareCondition.format.writes(condition),
            "leftOperand"  -> CompareCondition.format.writes(condition),
            "rightOperand" -> CompareCondition.format.writes(condition)
          )
        )
      case condition: CompositeCondition =>
        JsObject(
          Seq(
            "_type"          -> JsString("CompositeCondition"),
            "predicate"      -> CompositeCondition.format.writes(condition),
            "leftCondition"  -> CompositeCondition.format.writes(condition),
            "rightCondition" -> CompositeCondition.format.writes(condition)
          )
        )
    }
  )
}

object EffectDecisions extends Enumeration {
  type Decision = Value

  val Deny, Permit, Indeterminate, NonApplicable: Decision = Value

  implicit val format: Format[Decision] = Json.formatEnum(this)
}

object CombiningAlgorithms extends Enumeration {
  type Algorithm = Value

  val DenyOverride, PermitOverride: Algorithm = Value

  implicit val format: Format[Algorithm] = Json.formatEnum(this)
}

sealed trait Effect
case class PositiveEffect(decision: EffectDecisions.Decision) extends Effect
case class NegativeEffect(decision: EffectDecisions.Decision) extends Effect

object PositiveEffect {
  implicit val format: Format[PositiveEffect] = Json.format[PositiveEffect]
}

object NegativeEffect {
  implicit val format: Format[NegativeEffect] = Json.format[NegativeEffect]
}

object Effect {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[Effect] = Format[Effect] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "_type").read[String].reads(js)
      valueType.fold(
        _ => JsError("type undefined or incorrect"),
        {
          case "PositiveEffect" => JsPath.read[PositiveEffect].reads(js)
          case "NegativeEffect" => JsPath.read[NegativeEffect].reads(js)
        }
      )
    },
    Writes {
      case effect: PositiveEffect =>
        JsObject(
          Seq(
            "_type" -> JsString("PositiveEffect"),
            "id"    -> PositiveEffect.format.writes(effect)
          )
        )
      case effect: NegativeEffect =>
        JsObject(
          Seq(
            "_type" -> JsString("NegativeEffect"),
            "value" -> NegativeEffect.format.writes(effect)
          )
        )
    }
  )
}

abstract class ExpressionParameterValue
case class AttributeParameterValue(id: String) extends ExpressionParameterValue
case class BoolParameterValue(value: Boolean) extends ExpressionParameterValue
case class IntParameterValue(value: Int) extends ExpressionParameterValue
case class StringParameterValue(value: String) extends ExpressionParameterValue

object ExpressionParameterValue {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[ExpressionParameterValue] = Format[ExpressionParameterValue] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "_type").read[String].reads(js)
      valueType.fold(
        _ => JsError("type undefined or incorrect"),
        {
          case "AttributeValue" => JsPath.read[AttributeParameterValue].reads(js)
          case "BoolValue"      => JsPath.read[BoolParameterValue].reads(js)
          case "IntValue"       => JsPath.read[IntParameterValue].reads(js)
          case "StringValue"    => JsPath.read[StringParameterValue].reads(js)
        }
      )
    },
    Writes {
      case attr: AttributeParameterValue =>
        JsObject(
          Seq(
            "_type" -> JsString("AttributeValue"),
            "id"    -> AttributeParameterValue.format.writes(attr)
          )
        )
      case boolVal: BoolParameterValue =>
        JsObject(
          Seq(
            "_type" -> JsString("BoolValue"),
            "value" -> BoolParameterValue.format.writes(boolVal)
          )
        )
      case intVal: IntParameterValue =>
        JsObject(
          Seq(
            "_type" -> JsString("IntValue"),
            "value" -> IntParameterValue.format.writes(intVal)
          )
        )
      case stringVal: StringParameterValue =>
        JsObject(
          Seq(
            "_type" -> JsString("StringValue"),
            "value" -> StringParameterValue.format.writes(stringVal)
          )
        )
    }
  )
}

object AttributeParameterValue {
  implicit val format: Format[AttributeParameterValue] = Json.format[AttributeParameterValue]
}

object BoolParameterValue {
  implicit val format: Format[BoolParameterValue] = Json.format[BoolParameterValue]
}

object IntParameterValue {
  implicit val format: Format[IntParameterValue] = Json.format[IntParameterValue]
}

object StringParameterValue {
  implicit val format: Format[StringParameterValue] = Json.format[StringParameterValue]
}

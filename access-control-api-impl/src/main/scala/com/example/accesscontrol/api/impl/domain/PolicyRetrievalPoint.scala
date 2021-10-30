package com.example.accesscontrol.api.impl.domain

import play.api.libs.json.{Format, Json}

import scala.concurrent.Future

trait PolicyRetrievalPoint {
  case class PolicyCollectionParsingError(errorMessage: String) extends RuntimeException

  def buildPolicyCollection(): Future[Either[PolicyCollectionParsingError, PolicyCollection]]
}

trait TargetedPolicy {
  val target: Target
  val policy: Policy
}

trait PolicyCollection {
  val policySets: Array[_ <: PolicySet]
}

trait WithTargetType {
  val target: TargetType
}

trait PolicySet extends WithTargetType {
  val target: TargetType
  val combiningAlgorithm: CombiningAlgorithms.Algorithm
  val policies: Array[_ <: Policy]
}

trait TargetType {
  val value: String
}

trait Policy extends WithTargetType {
  val target: TargetType
  val combiningAlgorithm: CombiningAlgorithms.Algorithm
  val rules: Array[_ <: Rule]
}

trait Rule {
  val target: TargetType
  val condition: Condition
  val positiveEffect: PositiveEffect
  val negativeEffect: NegativeEffect
}

object CombiningAlgorithms extends Enumeration {
  type Algorithm = Value

  val DenyOverride, PermitOverride: Algorithm = Value

  implicit val format: Format[Algorithm] = Json.formatEnum(this)
}

sealed trait Condition
case class CompareCondition(operation: Operations.Operation, leftOperand: ExpressionParameterValue, rightOperand: ExpressionParameterValue) extends Condition
case class CompositeCondition(predicate: Predicates.Predicate, leftCondition: Condition, rightCondition: Condition) extends Condition

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

object Operations extends Enumeration {
  type Operation = Value

  val eq, lt, lte, gt, gte: Operation = Value

  implicit val format: Format[Operation] = Json.formatEnum(this)
}

object Predicates extends Enumeration {
  type Predicate = Value

  val AND, OR: Predicate = Value

  implicit val format: Format[Predicate] = Json.formatEnum(this)
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

object EffectDecisions extends Enumeration {
  type Decision = Value

  val Deny, Permit, Indeterminate, NonApplicable: Decision = Value

  implicit val format: Format[Decision] = Json.formatEnum(this)
}

sealed trait ExpressionParameterValue
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

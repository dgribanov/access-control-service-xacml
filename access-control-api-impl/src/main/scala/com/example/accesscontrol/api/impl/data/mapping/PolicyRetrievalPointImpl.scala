package com.example.accesscontrol.api.impl.data.mapping

import com.example.accesscontrol.api.impl.domain.{
  CombiningAlgorithms,
  Condition,
  EffectDecisions,
  ExpressionParameterValue,
  Operations,
  Policy,
  PolicyCollection,
  PolicyRepository,
  PolicyRetrievalPoint,
  PolicySet,
  Predicates,
  Rule,
  TargetType,
  Effect
}
import play.api.libs.json.{Format, JsError, JsSuccess, Json}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

final class PolicyRetrievalPointImpl @Inject() (policyRepository: PolicyRepository) extends PolicyRetrievalPoint {
  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future

  def buildPolicyCollection(): Future[Either[PolicyCollectionParsingError, PolicyCollection]] = {
    Future {
      Json.fromJson[PolicyCollectionSerializable](policyRepository.fetchPolicyCollection) match {
        case JsSuccess(policyCollection, _) => Right(policyCollection)
        case JsError.Message(errMsg)        => Left(PolicyCollectionParsingError(errMsg))
      }
    }
  }
}

object PolicyCollectionSerializable {
  implicit val format: Format[PolicyCollectionSerializable] = Json.format[PolicyCollectionSerializable]
}

case class PolicyCollectionSerializable(policySets: Array[PolicySetSerializable]) extends PolicyCollection

object PolicySetSerializable {
  implicit val format: Format[PolicySetSerializable] = Json.format[PolicySetSerializable]
}

case class PolicySetSerializable(target: TargetTypeSerializable, combiningAlgorithm: CombiningAlgorithms.Algorithm, policies: Array[PolicySerializable]) extends PolicySet

object PolicySerializable {
  implicit val format: Format[PolicySerializable] = Json.format[PolicySerializable]
}

case class PolicySerializable(target: TargetTypeSerializable, combiningAlgorithm: CombiningAlgorithms.Algorithm, rules: Array[RuleSerializable]) extends Policy

object RuleSerializable {
  implicit val format: Format[RuleSerializable] = Json.format[RuleSerializable]
}

case class RuleSerializable(target: TargetTypeSerializable, condition: ConditionSerializable, positiveEffect: PositiveEffect, negativeEffect: NegativeEffect) extends Rule

abstract class TargetTypeSerializable extends TargetType
case class ObjectTypeTarget(value: String) extends TargetTypeSerializable
case class ActionTypeTarget(value: String) extends TargetTypeSerializable
case class AttributeTypeTarget(value: String) extends TargetTypeSerializable

object ObjectTypeTarget {
  implicit val format: Format[ObjectTypeTarget] = Json.format[ObjectTypeTarget]
}

object ActionTypeTarget {
  implicit val format: Format[ActionTypeTarget] = Json.format[ActionTypeTarget]
}

object AttributeTypeTarget {
  implicit val format: Format[AttributeTypeTarget] = Json.format[AttributeTypeTarget]
}

object TargetTypeSerializable {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[TargetTypeSerializable] = Format[TargetTypeSerializable] (
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

abstract class ConditionSerializable extends Condition
case class CompareCondition(operation: Operations.Operation, leftOperand: ExpressionParameterValueSerializable, rightOperand: ExpressionParameterValueSerializable) extends ConditionSerializable
case class CompositeCondition(predicate: Predicates.Predicate, leftCondition: ConditionSerializable, rightCondition: ConditionSerializable) extends ConditionSerializable

object CompareCondition {
  implicit val format: Format[CompareCondition] = Json.format[CompareCondition]
}

object CompositeCondition {
  implicit val format: Format[CompositeCondition] = Json.format[CompositeCondition]
}

object ConditionSerializable {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[ConditionSerializable] = Format[ConditionSerializable] (
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

case class PositiveEffect(decision: EffectDecisions.Decision) extends Effect
case class NegativeEffect(decision: EffectDecisions.Decision) extends Effect

object PositiveEffect {
  implicit val format: Format[PositiveEffect] = Json.format[PositiveEffect]
}

object NegativeEffect {
  implicit val format: Format[NegativeEffect] = Json.format[NegativeEffect]
}

abstract class ExpressionParameterValueSerializable extends ExpressionParameterValue
case class AttributeParameterValue(id: String) extends ExpressionParameterValueSerializable
case class BoolParameterValue(value: Boolean) extends ExpressionParameterValueSerializable
case class IntParameterValue(value: Int) extends ExpressionParameterValueSerializable
case class StringParameterValue(value: String) extends ExpressionParameterValueSerializable

object ExpressionParameterValueSerializable {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[ExpressionParameterValueSerializable] = Format[ExpressionParameterValueSerializable] (
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

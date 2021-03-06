package com.example.accesscontrol.api.impl.data.mapping

import com.example.accesscontrol.api.impl.domain.{
  ActionTypeTarget,
  AttributeParameterValue,
  AttributeTypeTarget,
  BoolParameterValue,
  CombiningAlgorithms,
  CompareCondition,
  CompositeCondition,
  Condition,
  Effect,
  EffectDecisions,
  ExpressionParameterValue,
  IntParameterValue,
  ObjectTypeTarget,
  Operations,
  Policy,
  PolicyCollection,
  PolicySet,
  Predicates,
  Rule,
  StringParameterValue,
  TargetType
}

import play.api.libs.json.{Format, Json}

object PolicyCollectionSerializable {
  implicit val format: Format[PolicyCollectionSerializable] = Json.format[PolicyCollectionSerializable]
}

case class PolicyCollectionSerializable(id: String, version: String, policySets: Array[PolicySetSerializable]) extends PolicyCollection

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
case class ObjectTypeTargetSerializable(value: String) extends TargetTypeSerializable with ObjectTypeTarget
case class ActionTypeTargetSerializable(value: String) extends TargetTypeSerializable with ActionTypeTarget
case class AttributeTypeTargetSerializable(value: String) extends TargetTypeSerializable with AttributeTypeTarget

object ObjectTypeTargetSerializable {
  implicit val format: Format[ObjectTypeTargetSerializable] = Json.format[ObjectTypeTargetSerializable]
}

object ActionTypeTargetSerializable {
  implicit val format: Format[ActionTypeTargetSerializable] = Json.format[ActionTypeTargetSerializable]
}

object AttributeTypeTargetSerializable {
  implicit val format: Format[AttributeTypeTargetSerializable] = Json.format[AttributeTypeTargetSerializable]
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
          case "ObjectTypeTarget"    => JsPath.read[ObjectTypeTargetSerializable].reads(js)
          case "ActionTypeTarget"    => JsPath.read[ActionTypeTargetSerializable].reads(js)
          case "AttributeTypeTarget" => JsPath.read[AttributeTypeTargetSerializable].reads(js)
        }
      )
    },
    Writes {
      case target: ObjectTypeTargetSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("ObjectTypeTarget"),
            "id"    -> ObjectTypeTargetSerializable.format.writes(target)
          )
        )
      case target: ActionTypeTargetSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("ActionTypeTarget"),
            "value" -> ActionTypeTargetSerializable.format.writes(target)
          )
        )
      case target: AttributeTypeTargetSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("AttributeTypeTarget"),
            "value" -> AttributeTypeTargetSerializable.format.writes(target)
          )
        )
    }
  )
}

abstract class ConditionSerializable extends Condition
case class CompareConditionSerializable(operation: Operations.Operation, leftOperand: ExpressionParameterValueSerializable, rightOperand: ExpressionParameterValueSerializable) extends ConditionSerializable with CompareCondition
case class CompositeConditionSerializable(predicate: Predicates.Predicate, leftCondition: ConditionSerializable, rightCondition: ConditionSerializable) extends ConditionSerializable with CompositeCondition

object CompareConditionSerializable {
  implicit val format: Format[CompareConditionSerializable] = Json.format[CompareConditionSerializable]
}

object CompositeConditionSerializable {
  implicit val format: Format[CompositeConditionSerializable] = Json.format[CompositeConditionSerializable]
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
          case "CompareCondition"   => JsPath.read[CompareConditionSerializable].reads(js)
          case "CompositeCondition" => JsPath.read[CompositeConditionSerializable].reads(js)
        }
      )
    },
    Writes {
      case condition: CompareConditionSerializable =>
        JsObject(
          Seq(
            "_type"        -> JsString("CompareCondition"),
            "operation"    -> CompareConditionSerializable.format.writes(condition),
            "leftOperand"  -> CompareConditionSerializable.format.writes(condition),
            "rightOperand" -> CompareConditionSerializable.format.writes(condition)
          )
        )
      case condition: CompositeConditionSerializable =>
        JsObject(
          Seq(
            "_type"          -> JsString("CompositeCondition"),
            "predicate"      -> CompositeConditionSerializable.format.writes(condition),
            "leftCondition"  -> CompositeConditionSerializable.format.writes(condition),
            "rightCondition" -> CompositeConditionSerializable.format.writes(condition)
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
case class AttributeParameterValueSerializable(id: String) extends ExpressionParameterValueSerializable with AttributeParameterValue
case class BoolParameterValueSerializable(value: Boolean) extends ExpressionParameterValueSerializable with BoolParameterValue
case class IntParameterValueSerializable(value: Int) extends ExpressionParameterValueSerializable with IntParameterValue
case class StringParameterValueSerializable(value: String) extends ExpressionParameterValueSerializable with StringParameterValue

object ExpressionParameterValueSerializable {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[ExpressionParameterValueSerializable] = Format[ExpressionParameterValueSerializable] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "_type").read[String].reads(js)
      valueType.fold(
        _ => JsError("type undefined or incorrect"),
        {
          case "AttributeValue" => JsPath.read[AttributeParameterValueSerializable].reads(js)
          case "BoolValue"      => JsPath.read[BoolParameterValueSerializable].reads(js)
          case "IntValue"       => JsPath.read[IntParameterValueSerializable].reads(js)
          case "StringValue"    => JsPath.read[StringParameterValueSerializable].reads(js)
        }
      )
    },
    Writes {
      case attr: AttributeParameterValueSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("AttributeValue"),
            "id"    -> AttributeParameterValueSerializable.format.writes(attr)
          )
        )
      case boolVal: BoolParameterValueSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("BoolValue"),
            "value" -> BoolParameterValueSerializable.format.writes(boolVal)
          )
        )
      case intVal: IntParameterValueSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("IntValue"),
            "value" -> IntParameterValueSerializable.format.writes(intVal)
          )
        )
      case stringVal: StringParameterValueSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("StringValue"),
            "value" -> StringParameterValueSerializable.format.writes(stringVal)
          )
        )
    }
  )
}

object AttributeParameterValueSerializable {
  implicit val format: Format[AttributeParameterValueSerializable] = Json.format[AttributeParameterValueSerializable]
}

object BoolParameterValueSerializable {
  implicit val format: Format[BoolParameterValueSerializable] = Json.format[BoolParameterValueSerializable]
}

object IntParameterValueSerializable {
  implicit val format: Format[IntParameterValueSerializable] = Json.format[IntParameterValueSerializable]
}

object StringParameterValueSerializable {
  implicit val format: Format[StringParameterValueSerializable] = Json.format[StringParameterValueSerializable]
}

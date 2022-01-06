package com.example.accesscontrol.admin.ws.api.impl.domain

import play.api.libs.json.{Format, Json}

case class PolicySetSerializable(target: TargetTypeSerializable, combiningAlgorithm: CombiningAlgorithms.Algorithm, policies: Array[PolicySerializable]) extends PolicySet

object PolicySetSerializable {
  implicit val format: Format[PolicySetSerializable] = Json.format[PolicySetSerializable]
}

case class PolicySerializable(target: TargetTypeSerializable, combiningAlgorithm: CombiningAlgorithms.Algorithm, rules: Array[RuleSerializable]) extends Policy

object PolicySerializable {
  implicit val format: Format[PolicySerializable] = Json.format[PolicySerializable]
}

case class RuleSerializable(target: TargetTypeSerializable, condition: ConditionSerializable, positiveEffect: PositiveEffectSerializable, negativeEffect: NegativeEffectSerializable) extends Rule

object RuleSerializable {
  implicit val format: Format[RuleSerializable] = Json.format[RuleSerializable]
}

abstract class TargetTypeSerializable extends TargetType
case class ObjectTypeTargetSerializable(value: String) extends TargetTypeSerializable
case class ActionTypeTargetSerializable(value: String) extends TargetTypeSerializable
case class AttributeTypeTargetSerializable(value: String) extends TargetTypeSerializable

object ObjectTypeTargetSerializable {
  implicit val format: Format[ObjectTypeTargetSerializable] = Json.format[ObjectTypeTargetSerializable]

  def convert: PartialFunction[TargetType, TargetTypeSerializable] = {
    case t: TargetType => ObjectTypeTargetSerializable(t.value)
  }
}

object ActionTypeTargetSerializable {
  implicit val format: Format[ActionTypeTargetSerializable] = Json.format[ActionTypeTargetSerializable]

  def convert: PartialFunction[TargetType, TargetTypeSerializable] = {
    case t: TargetType => ActionTypeTargetSerializable(t.value)
  }
}

object AttributeTypeTargetSerializable {
  implicit val format: Format[AttributeTypeTargetSerializable] = Json.format[AttributeTypeTargetSerializable]

  def convert: PartialFunction[TargetType, TargetTypeSerializable] = {
    case t: TargetType => AttributeTypeTargetSerializable(t.value)
  }
}

object TargetTypeSerializable {
  import play.api.libs.json.{JsError, JsObject, JsPath, JsString, Reads, Writes}

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

case class CombineRulesAlgorithmSerializable(algorithm: String)

object CombineRulesAlgorithmSerializable {
  implicit val format: Format[CombineRulesAlgorithmSerializable] = Json.format[CombineRulesAlgorithmSerializable]
}

sealed trait ConditionSerializable extends Condition
case class CompareConditionSerializable(operation: String, leftOperand: ExpressionParameterValueSerializable, rightOperand: ExpressionParameterValueSerializable) extends ConditionSerializable
case class CompositeConditionSerializable(predicate: String, leftCondition: ConditionSerializable, rightCondition: ConditionSerializable) extends ConditionSerializable

object CompareConditionSerializable {
  implicit val format: Format[CompareConditionSerializable] = Json.format[CompareConditionSerializable]
}

object CompositeConditionSerializable {
  implicit val format: Format[CompositeConditionSerializable] = Json.format[CompositeConditionSerializable]
}

object ConditionSerializable {
  import play.api.libs.json.{JsError, JsObject, JsPath, JsString, Reads, Writes}

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

sealed trait EffectSerializable extends Effect
case class PositiveEffectSerializable(decision: EffectDecisions.Decision) extends EffectSerializable
case class NegativeEffectSerializable(decision: EffectDecisions.Decision) extends EffectSerializable

object PositiveEffectSerializable {
  implicit val format: Format[PositiveEffectSerializable] = Json.format[PositiveEffectSerializable]
}

object NegativeEffectSerializable {
  implicit val format: Format[NegativeEffectSerializable] = Json.format[NegativeEffectSerializable]
}

object EffectSerializable {
  import play.api.libs.json.{JsError, JsObject, JsPath, JsString, Reads, Writes}

  implicit val format: Format[EffectSerializable] = Format[EffectSerializable] (
    Reads { js =>
      // use the _type field to determine how to deserialize
      val valueType = (JsPath \ "_type").read[String].reads(js)
      valueType.fold(
        _ => JsError("type undefined or incorrect"),
        {
          case "PositiveEffect" => JsPath.read[PositiveEffectSerializable].reads(js)
          case "NegativeEffect" => JsPath.read[NegativeEffectSerializable].reads(js)
        }
      )
    },
    Writes {
      case effect: PositiveEffectSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("PositiveEffect"),
            "id"    -> PositiveEffectSerializable.format.writes(effect)
          )
        )
      case effect: NegativeEffectSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("NegativeEffect"),
            "value" -> NegativeEffectSerializable.format.writes(effect)
          )
        )
    }
  )
}

abstract class ExpressionParameterValueSerializable
case class AttributeParameterValueSerializable(id: String) extends ExpressionParameterValueSerializable
case class BoolParameterValueSerializable(value: Boolean) extends ExpressionParameterValueSerializable
case class IntParameterValueSerializable(value: Int) extends ExpressionParameterValueSerializable
case class StringParameterValueSerializable(value: String) extends ExpressionParameterValueSerializable

object ExpressionParameterValueSerializable {
  import play.api.libs.json.{JsError, JsObject, JsPath, JsString, Reads, Writes}

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

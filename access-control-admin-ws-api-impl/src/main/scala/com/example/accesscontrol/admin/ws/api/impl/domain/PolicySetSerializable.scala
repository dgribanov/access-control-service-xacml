package com.example.accesscontrol.admin.ws.api.impl.domain

import play.api.libs.json.{Format, Json}

case class PolicySetSerializable(target: TargetTypeSerializable, combiningAlgorithm: CombiningAlgorithms.Algorithm, policies: Array[PolicySerializable]) extends PolicySet

object PolicySetSerializable {
  def convert: PartialFunction[PolicySet, PolicySetSerializable] = {
    case ps: PolicySet => PolicySetSerializable(TargetTypeSerializable.convert(ps.target), ps.combiningAlgorithm, ps.policies map PolicySerializable.convert)
  }

  implicit val format: Format[PolicySetSerializable] = Json.format[PolicySetSerializable]
}

case class PolicySerializable(target: TargetTypeSerializable, combiningAlgorithm: CombiningAlgorithms.Algorithm, rules: Array[RuleSerializable]) extends Policy

object PolicySerializable {
  def convert: PartialFunction[Policy, PolicySerializable] = {
    case p: Policy => PolicySerializable(TargetTypeSerializable.convert(p.target), p.combiningAlgorithm, p.rules map RuleSerializable.convert)
  }

  implicit val format: Format[PolicySerializable] = Json.format[PolicySerializable]
}

case class RuleSerializable(target: TargetTypeSerializable, condition: ConditionSerializable, positiveEffect: PositiveEffectSerializable, negativeEffect: NegativeEffectSerializable) extends Rule

object RuleSerializable {
  def convert: PartialFunction[Rule, RuleSerializable] = {
    case r: Rule => RuleSerializable(TargetTypeSerializable.convert(r.target), ConditionSerializable.convert(r.condition), PositiveEffectSerializable.convert(r.positiveEffect), NegativeEffectSerializable.convert(r.negativeEffect))
  }

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

  def convert: PartialFunction[TargetType, TargetTypeSerializable] = {
    case t: ObjectTypeTarget    => ObjectTypeTargetSerializable(t.value)
    case t: ActionTypeTarget    => ActionTypeTargetSerializable(t.value)
    case t: AttributeTypeTarget => AttributeTypeTargetSerializable(t.value)
  }

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
            "value" -> JsString(target.value)
          )
        )
      case target: ActionTypeTargetSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("ActionTypeTarget"),
            "value" -> JsString(target.value)
          )
        )
      case target: AttributeTypeTargetSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("AttributeTypeTarget"),
            "value" -> JsString(target.value)
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
case class CompareConditionSerializable(operation: Operations.Operation, leftOperand: ExpressionParameterValueSerializable, rightOperand: ExpressionParameterValueSerializable) extends ConditionSerializable
case class CompositeConditionSerializable(predicate: Predicates.Predicate, leftCondition: ConditionSerializable, rightCondition: ConditionSerializable) extends ConditionSerializable

object CompareConditionSerializable {
  implicit val format: Format[CompareConditionSerializable] = Json.format[CompareConditionSerializable]
}

object CompositeConditionSerializable {
  implicit val format: Format[CompositeConditionSerializable] = Json.format[CompositeConditionSerializable]
}

object ConditionSerializable {
  import play.api.libs.json.{JsError, JsObject, JsPath, JsString, Reads, Writes}

  def convert: PartialFunction[Condition, ConditionSerializable] = {
    case c: CompareCondition =>
      CompareConditionSerializable(
        c.operation,
        ExpressionParameterValueSerializable.convert(c.leftOperand),
        ExpressionParameterValueSerializable.convert(c.rightOperand)
      )
    case c: CompositeCondition =>
      CompositeConditionSerializable(
        c.predicate,
        ConditionSerializable.convert(c.leftCondition),
        ConditionSerializable.convert(c.rightCondition)
      )
  }

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
            "operation"    -> Operations.format.writes(condition.operation),
            "leftOperand"  -> ExpressionParameterValueSerializable.format.writes(condition.leftOperand),
            "rightOperand" -> ExpressionParameterValueSerializable.format.writes(condition.rightOperand)
          )
        )
      case condition: CompositeConditionSerializable =>
        JsObject(
          Seq(
            "_type"          -> JsString("CompositeCondition"),
            "predicate"      -> Predicates.format.writes(condition.predicate),
            "leftCondition"  -> ConditionSerializable.format.writes(condition.leftCondition),
            "rightCondition" -> ConditionSerializable.format.writes(condition.rightCondition)
          )
        )
    }
  )
}

sealed trait EffectSerializable extends Effect
case class PositiveEffectSerializable(decision: EffectDecisions.Decision) extends EffectSerializable
case class NegativeEffectSerializable(decision: EffectDecisions.Decision) extends EffectSerializable

object PositiveEffectSerializable {
  def convert: PartialFunction[Effect, PositiveEffectSerializable] = {
    case e: Effect => PositiveEffectSerializable(e.decision)
  }

  implicit val format: Format[PositiveEffectSerializable] = Json.format[PositiveEffectSerializable]
}

object NegativeEffectSerializable {
  def convert: PartialFunction[Effect, NegativeEffectSerializable] = {
    case e: Effect => NegativeEffectSerializable(e.decision)
  }

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
            "_type"    -> JsString("PositiveEffect"),
            "decision" -> EffectDecisions.format.writes(effect.decision)
          )
        )
      case effect: NegativeEffectSerializable =>
        JsObject(
          Seq(
            "_type"    -> JsString("NegativeEffect"),
            "decision" -> EffectDecisions.format.writes(effect.decision)
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
  import play.api.libs.json.{JsError, JsObject, JsPath, JsString, JsBoolean, JsNumber, Reads, Writes}

  def convert: PartialFunction[ExpressionParameterValue, ExpressionParameterValueSerializable] = {
    case a: AttributeParameterValue => AttributeParameterValueSerializable(a.id)
    case b: BoolParameterValue      => BoolParameterValueSerializable(b.value)
    case i: IntParameterValue       => IntParameterValueSerializable(i.value)
    case s: StringParameterValue    => StringParameterValueSerializable(s.value)
  }

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
            "id"    -> JsString(attr.id)
          )
        )
      case boolVal: BoolParameterValueSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("BoolValue"),
            "value" -> JsBoolean(boolVal.value)
          )
        )
      case intVal: IntParameterValueSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("IntValue"),
            "value" -> JsNumber(intVal.value)
          )
        )
      case stringVal: StringParameterValueSerializable =>
        JsObject(
          Seq(
            "_type" -> JsString("StringValue"),
            "value" -> JsString(stringVal.value)
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

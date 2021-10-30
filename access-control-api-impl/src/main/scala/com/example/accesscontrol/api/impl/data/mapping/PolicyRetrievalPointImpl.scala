package com.example.accesscontrol.api.impl.data.mapping

import com.example.accesscontrol.api.impl.domain.{CombiningAlgorithms, Condition, NegativeEffect, Policy, PolicyCollection, PolicyRepository, PolicyRetrievalPoint, PolicySet, PositiveEffect, Rule, TargetType}
import play.api.libs.json.{Format, JsError, JsSuccess, Json}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

final class PolicyRetrievalPointImpl @Inject() (policyRepository: PolicyRepository) extends PolicyRetrievalPoint {
  implicit val ec: ExecutionContext = ExecutionContext.global // don`t move! it`s implicit ExecutionContext for Future

  def buildPolicyCollection(): Future[Either[PolicyCollectionParsingError, PolicyCollection]] = {
    Future {
      Json.fromJson[PolicyCollectionSerializableImpl](policyRepository.fetchPolicyCollection) match {
        case JsSuccess(policyCollection, _) => Right(policyCollection)
        case JsError.Message(errMsg)        => Left(PolicyCollectionParsingError(errMsg))
      }
    }
  }
}

object PolicyCollectionSerializableImpl {
  implicit val format: Format[PolicyCollectionSerializableImpl] = Json.format[PolicyCollectionSerializableImpl]
}

case class PolicyCollectionSerializableImpl(policySets: Array[PolicySetSerializableImpl]) extends PolicyCollection

object PolicySetSerializableImpl {
  implicit val format: Format[PolicySetSerializableImpl] = Json.format[PolicySetSerializableImpl]
}

case class PolicySetSerializableImpl(target: TargetTypeImpl, combiningAlgorithm: CombiningAlgorithms.Algorithm, policies: Array[PolicySerializableImpl]) extends PolicySet

object PolicySerializableImpl {
  implicit val format: Format[PolicySerializableImpl] = Json.format[PolicySerializableImpl]
}

case class PolicySerializableImpl(target: TargetTypeImpl, combiningAlgorithm: CombiningAlgorithms.Algorithm, rules: Array[RuleSerializableImpl]) extends Policy

object RuleSerializableImpl {
  implicit val format: Format[RuleSerializableImpl] = Json.format[RuleSerializableImpl]
}

case class RuleSerializableImpl(target: TargetTypeImpl, condition: Condition, positiveEffect: PositiveEffect, negativeEffect: NegativeEffect) extends Rule

abstract class TargetTypeImpl extends TargetType
case class ObjectTypeTarget(value: String) extends TargetTypeImpl
case class ActionTypeTarget(value: String) extends TargetTypeImpl
case class AttributeTypeTarget(value: String) extends TargetTypeImpl

object ObjectTypeTarget {
  implicit val format: Format[ObjectTypeTarget] = Json.format[ObjectTypeTarget]
}

object ActionTypeTarget {
  implicit val format: Format[ActionTypeTarget] = Json.format[ActionTypeTarget]
}

object AttributeTypeTarget {
  implicit val format: Format[AttributeTypeTarget] = Json.format[AttributeTypeTarget]
}

object TargetTypeImpl {
  import play.api.libs.json.{Reads, Writes, JsPath, JsError, JsObject, JsString}

  implicit val format: Format[TargetTypeImpl] = Format[TargetTypeImpl] (
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

package com.example.accesscontrol.api.impl.domain

/**
 * Inspired by scala.math.PartiallyOrdered idea for compare objects
 * but with more correct option result of compare methods
 */
sealed trait ExpressionValue[+T] {
  def equals[A >: T](obj: A): Option[Boolean]

  def tryCompareTo [B >: T](that: B): Option[Int]

  def < [B >: T](that: B): Option[Boolean] =
    this tryCompareTo that match {
      case Some(x) if x < 0  => Some(true)
      case Some(x) if x >= 0 => Some(false)
      case None              => None
    }

  def > [B >: T](that: B): Option[Boolean] =
    this tryCompareTo that match {
      case Some(x) if x > 0  => Some(true)
      case Some(x) if x <= 0 => Some(false)
      case None              => None
    }

  def <= [B >: T](that: B): Option[Boolean] =
    this tryCompareTo that match {
      case Some(x) if x <= 0 => Some(true)
      case Some(x) if x > 0  => Some(false)
      case None              => None
    }

  def >= [B >: T](that: B): Option[Boolean] =
    this tryCompareTo that match {
      case Some(x) if x >= 0 => Some(true)
      case Some(x) if x < 0  => Some(false)
      case None              => None
    }
}

object ExpressionValue {
  def apply(paramValue: ExpressionParameterValue)(implicit attributes: Array[Attribute]): ExpressionValue[Any] =
    paramValue match {
      case AttributeParameterValueImpl(id) => AttributeExpressionValue(id, attributes)
      case BoolParameterValueImpl(value)   => BoolExpressionValue(value)
      case IntParameterValueImpl(value)    => IntExpressionValue(value)
      case StringParameterValueImpl(value) => StringExpressionValue(value)
    }

  abstract case class AttributeExpressionValue(value: ExpressionValue[Any]) extends ExpressionValue[AttributeExpressionValue] {
    override def equals[A >: AttributeExpressionValue](obj: A): Option[Boolean] =
      value equals obj

    override def tryCompareTo[B >: AttributeExpressionValue](that: B): Option[Int] =
      value tryCompareTo that
  }

  abstract case class BoolExpressionValue(value: Boolean) extends ExpressionValue[BoolExpressionValue] {
    override def equals[A >: BoolExpressionValue](that: A): Option[Boolean] =
      that match {
        case BoolExpressionValue(v)            => Some(value == v) // compare Boolean and Boolean
        case IntExpressionValue                => None // don`t compare Boolean and Int
        case StringExpressionValue             => None // don`t compare Boolean and String
        case attrVal: AttributeExpressionValue => attrVal equals this // compare Boolean and Attribute
      }

    override def tryCompareTo[B >: BoolExpressionValue](that: B): Option[Int] = None
  }

  abstract case class IntExpressionValue(value: Int) extends ExpressionValue[IntExpressionValue] {
    override def equals[A >: IntExpressionValue](obj: A): Option[Boolean] =
      obj match {
        case BoolExpressionValue               => None // don`t compare Int and Boolean
        case IntExpressionValue(v)             => Some(value == v) // compare Int and Int
        case StringExpressionValue             => None // don`t compare Int and String
        case attrVal: AttributeExpressionValue => attrVal equals this // compare Int and Attribute
      }

    override def tryCompareTo[B >: IntExpressionValue](that: B): Option[Int] =
      that match {
        case BoolExpressionValue               => None // don`t compare Int and Boolean
        case IntExpressionValue(v)             => Some(value - v) // compare Int and Int
        case StringExpressionValue             => None // don`t compare Int and String
        case attrVal: AttributeExpressionValue => attrVal tryCompareTo this // compare Int and Attribute
      }
  }

  abstract case class StringExpressionValue(value: String) extends ExpressionValue[StringExpressionValue] {
    override def equals[A >: StringExpressionValue](obj: A): Option[Boolean] =
      obj match {
        case BoolExpressionValue               => None // don`t compare String and Boolean
        case IntExpressionValue                => None // don`t compare String and Int
        case StringExpressionValue(v)          => Some(value == v) // compare String and String
        case attrVal: AttributeExpressionValue => attrVal equals this // compare String and Attribute
      }

    override def tryCompareTo[B >: StringExpressionValue](that: B): Option[Int] = None
  }

  private case class EmptyExpressionValue() extends ExpressionValue[EmptyExpressionValue] {
    override def equals[A >: EmptyExpressionValue](obj: A): Option[Boolean] = None // don`t compare empty value and any other value

    override def tryCompareTo[B >: EmptyExpressionValue](that: B): Option[Int] = None // don`t compare empty value and any other value
  }

  object AttributeExpressionValue {
    def apply(id: String, attributes: Array[Attribute]): AttributeExpressionValue = {
      val value = attributes.foldRight[ExpressionValue[Any]](EmptyExpressionValue())(
        (attribute, emptyValue) => if (attribute.name == id) toExpressionValue(attribute.value) else emptyValue
      )
      new ExpressionValue.AttributeExpressionValue(value) {}
    }

    private def toExpressionValue(attributeValue: AttributeValue): ExpressionValue[Any] =
      attributeValue.value match {
        case value: String  => StringExpressionValue(value)
        case value: Boolean => BoolExpressionValue(value)
        case value: Int     => IntExpressionValue(value)
      }
  }

  object BoolExpressionValue {
    def apply(value: Boolean): BoolExpressionValue = new ExpressionValue.BoolExpressionValue(value) {}
  }

  object IntExpressionValue {
    def apply(value: Int): IntExpressionValue = new ExpressionValue.IntExpressionValue(value) {}
  }

  object StringExpressionValue {
    def apply(value: String): StringExpressionValue = new ExpressionValue.StringExpressionValue(value) {}
  }
}
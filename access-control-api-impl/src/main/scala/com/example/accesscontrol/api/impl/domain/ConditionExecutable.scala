package com.example.accesscontrol.api.impl.domain

object ConditionExecutable {
  def convert: PartialFunction[Condition, ConditionExecutable] = {
    case c: CompareCondition =>
      CompareConditionExecutable(
        c.operation,
        ExpressionParameterValueExecutable.convert(c.leftOperand),
        ExpressionParameterValueExecutable.convert(c.rightOperand)
      )
    case c: CompositeCondition =>
      CompositeConditionExecutable(
        c.predicate,
        ConditionExecutable.convert(c.leftCondition),
        ConditionExecutable.convert(c.rightCondition)
      )
  }
}

abstract class ConditionExecutable extends Condition
case class CompareConditionExecutable(operation: Operations.Operation, leftOperand: ExpressionParameterValueExecutable, rightOperand: ExpressionParameterValueExecutable) extends ConditionExecutable
case class CompositeConditionExecutable(predicate: Predicates.Predicate, leftCondition: ConditionExecutable, rightCondition: ConditionExecutable) extends ConditionExecutable

object ExpressionParameterValueExecutable {
  def convert: PartialFunction[ExpressionParameterValue, ExpressionParameterValueExecutable] = {
    case a: AttributeParameterValue => AttributeParameterValueExecutable(a.id)
    case b: BoolParameterValue      => BoolParameterValueExecutable(b.value)
    case i: IntParameterValue       => IntParameterValueExecutable(i.value)
    case s: StringParameterValue    => StringParameterValueExecutable(s.value)
  }
}

abstract class ExpressionParameterValueExecutable extends ExpressionParameterValue
case class AttributeParameterValueExecutable(id: String) extends ExpressionParameterValueExecutable
case class BoolParameterValueExecutable(value: Boolean) extends ExpressionParameterValueExecutable
case class IntParameterValueExecutable(value: Int) extends ExpressionParameterValueExecutable
case class StringParameterValueExecutable(value: String) extends ExpressionParameterValueExecutable

package com.colofabrix.scala.restbee

import scala.deriving.Mirror
import scala.compiletime.*

private[colofabrix] trait GetEncoder[A]:
  def toQueryParams(a: A): Map[String, String]

private[colofabrix] object GetEncoder:

  def apply[A](using ev: GetEncoder[A]): GetEncoder[A] = ev

  final inline def derived[A](using inline m: Mirror.Of[A]): GetEncoder[A] =
    inline m match
      case s: Mirror.SumOf[A]     => deriveSumType(using s)
      case p: Mirror.ProductOf[A] => deriveProductType(using p)

  //  ADT  //

  inline def deriveProductType[A](using m: Mirror.ProductOf[A]): GetEncoder[A] =
    new GetEncoder[A]:
      def toQueryParams(a: A): Map[String, String] =
        val elemLabels   = getElemLabels[m.MirroredElemLabels]
        val elemEncoders = getTypeclassInstances[m.MirroredElemTypes]
        val elems        = a.asInstanceOf[Product].productIterator

        (elems zip elemLabels zip elemEncoders)
          .flatMap {
            case ((elem, elemLabel), elemEncoder) => encodeElement(elem, elemLabel, elemEncoder)
          }
          .toMap

      private def encodeElement(elem: Any, elemLabel: String, elemEncoder: GetEncoder[Any]) =
        elemEncoder
          .toQueryParams(elem)
          .toList
          .map {
            case (k, v) =>
              val qualifiedKey = List(elemLabel, k).filter(_.nonEmpty).mkString(".")
              qualifiedKey -> v
          }

  inline def deriveSumType[A](using m: Mirror.SumOf[A]) =
    new GetEncoder[A]:
      def toQueryParams(a: A): Map[String, String] =
        val elemEncoders = getTypeclassInstances[m.MirroredElemTypes]
        val elemOrdinal  = m.ordinal(a)
        elemEncoders(elemOrdinal).toQueryParams(a)

  inline def getElemLabels[A <: Tuple]: List[String] =
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (head *: tail) =>
        val headElementLabel  = constValue[head].toString
        val tailElementLabels = getElemLabels[tail]
        headElementLabel :: tailElementLabels

  inline def getTypeclassInstances[A <: Tuple]: List[GetEncoder[Any]] =
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (head *: tail) =>
        val headTypeClass   = summonInline[GetEncoder[head]]
        val tailTypeClasses = getTypeclassInstances[tail]
        headTypeClass.asInstanceOf[GetEncoder[Any]] :: tailTypeClasses

  //  Aggregations  //

  given optionGetEncoder[A](using GetEncoder[A]): GetEncoder[Option[A]] with
    def toQueryParams(oa: Option[A]): Map[String, String] =
      oa.fold(Map.empty)(GetEncoder[A].toQueryParams)

  given iterableGetEncoder[A](using GetEncoder[A]): GetEncoder[Iterable[A]] with
    def toQueryParams(as: Iterable[A]): Map[String, String] =
      as.map(GetEncoder[A].toQueryParams).foldLeft(Map.empty)(_ ++ _)

  //  Primitive Types  //

  given stringGetEncoder: GetEncoder[String] with
    def toQueryParams(a: String): Map[String, String] =
      Map("" -> a)

  given intGetEncoder: GetEncoder[Int] with
    def toQueryParams(a: Int): Map[String, String] =
      Map("" -> a.toString())

  given longGetEncoder: GetEncoder[Long] with
    def toQueryParams(a: Long): Map[String, String] =
      Map("" -> a.toString())

  given doubleGetEncoder: GetEncoder[Double] with
    def toQueryParams(a: Double): Map[String, String] =
      Map("" -> a.toString())

  given booleanGetEncoder: GetEncoder[Boolean] with
    def toQueryParams(a: Boolean): Map[String, String] =
      Map("" -> a.toString())

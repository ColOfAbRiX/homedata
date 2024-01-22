package com.colofabrix.scala.restbee.encoding

/**
 * URL Query Parameters Encoding
 */
trait UrlParamsEncoder[A]:
  def encode(a: A): Map[String, String]

  extension (a: A)
    def toQueryParams: Map[String, String] =
      encode(a)

object UrlParamsEncoder:
  def apply[A](using ev: UrlParamsEncoder[A]): UrlParamsEncoder[A] = ev

  import scala.deriving.Mirror
  import scala.compiletime.*

  final inline def derived[A](using inline m: Mirror.Of[A]): UrlParamsEncoder[A] =
    inline m match
      case s: Mirror.SumOf[A]     => deriveSumType(using s)
      case p: Mirror.ProductOf[A] => deriveProductType(using p)

  //  ADT  //

  inline def deriveProductType[A](using m: Mirror.ProductOf[A]): UrlParamsEncoder[A] =
    new UrlParamsEncoder[A]:
      def encode(a: A): Map[String, String] =
        val elemLabels   = getElemLabels[m.MirroredElemLabels]
        val elemEncoders = getTypeclassInstances[m.MirroredElemTypes]
        val elems        = a.asInstanceOf[Product].productIterator

        (elems zip elemLabels zip elemEncoders)
          .flatMap {
            case ((elem, elemLabel), elemEncoder) => encodeElement(elem, elemLabel, elemEncoder)
          }
          .toMap

      private def encodeElement(elem: Any, elemLabel: String, elemEncoder: UrlParamsEncoder[Any]) =
        elemEncoder
          .encode(elem)
          .toList
          .map {
            case (k, v) =>
              val qualifiedKey = List(elemLabel, k).filter(_.nonEmpty).mkString(".")
              qualifiedKey -> v
          }

  inline def deriveSumType[A](using m: Mirror.SumOf[A]) =
    new UrlParamsEncoder[A]:
      def encode(a: A): Map[String, String] =
        val elemEncoders = getTypeclassInstances[m.MirroredElemTypes]
        val elemOrdinal  = m.ordinal(a)
        elemEncoders(elemOrdinal).encode(a)

  inline def getElemLabels[A <: Tuple]: List[String] =
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (head *: tail) =>
        val headElementLabel  = constValue[head].toString
        val tailElementLabels = getElemLabels[tail]
        headElementLabel :: tailElementLabels

  inline def getTypeclassInstances[A <: Tuple]: List[UrlParamsEncoder[Any]] =
    inline erasedValue[A] match
      case _: EmptyTuple => Nil
      case _: (head *: tail) =>
        val headTypeClass   = summonInline[UrlParamsEncoder[head]]
        val tailTypeClasses = getTypeclassInstances[tail]
        headTypeClass.asInstanceOf[UrlParamsEncoder[Any]] :: tailTypeClasses

  //  Aggregations  //

  given optionUrlParamsEncoder[A](using UrlParamsEncoder[A]): UrlParamsEncoder[Option[A]] with
    def encode(oa: Option[A]): Map[String, String] =
      oa.fold(Map.empty)(UrlParamsEncoder[A].encode)

  given iterableUrlParamsEncoder[A](using UrlParamsEncoder[A]): UrlParamsEncoder[Iterable[A]] with
    def encode(as: Iterable[A]): Map[String, String] =
      as.map(UrlParamsEncoder[A].encode).foldLeft(Map.empty)(_ ++ _)

  //  Primitive Types  //

  given stringUrlParamsEncoder: UrlParamsEncoder[String] with
    def encode(a: String): Map[String, String] = Map("" -> a)

  given intUrlParamsEncoder: UrlParamsEncoder[Int] with
    def encode(a: Int): Map[String, String] = Map("" -> a.toString())

  given longUrlParamsEncoder: UrlParamsEncoder[Long] with
    def encode(a: Long): Map[String, String] = Map("" -> a.toString())

  given doubleUrlParamsEncoder: UrlParamsEncoder[Double] with
    def encode(a: Double): Map[String, String] = Map("" -> a.toString())

  given booleanUrlParamsEncoder: UrlParamsEncoder[Boolean] with
    def encode(a: Boolean): Map[String, String] = Map("" -> a.toString())

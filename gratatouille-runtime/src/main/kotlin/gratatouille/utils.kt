package gratatouille

fun String.capitalizeFirstLetter(): String {
  val builder = StringBuilder(length)
  var isCapitalized = false
  forEach {
    builder.append(if (!isCapitalized && it.isLetter()) {
      isCapitalized = true
      it.toString().uppercase()
    } else {
      it.toString()
    })
  }
  return builder.toString()
}

fun String.decapitalizeFirstLetter(): String {
  val builder = StringBuilder(length)
  var isCapitalized = false
  forEach {
    builder.append(if (!isCapitalized && it.isLetter()) {
      isCapitalized = true
      it.toString().lowercase()
    } else {
      it.toString()
    })
  }
  return builder.toString()
}
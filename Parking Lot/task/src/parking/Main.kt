package parking

infix fun String.splitAt(n: Int) = listOf(take(n), drop(n + 1))
infix fun String.splitAt(ch: Char) = splitAt(indexOf(ch))

data class Car(val reg: String, val color: String)

val String.asCar: Car
    get() = trim().splitAt(' ').let { (id, color) -> Car(id, color) }

sealed class Command
data class Leave(val n: Int) : Command()
data class Park(val car: Car) : Command()
data class Create(val n: Int) : Command()
data class RegByColor(val color: String) : Command()
data class SpotByReg(val reg: String) : Command()
data class SpotByColor(val color: String) : Command()
object Status : Command()
object Exit : Command()

val String.asCommand: Command
    get() = when (this) {
        "status" -> Status
        "exit" -> Exit
        else -> splitAt(' ').let { (type, arg) ->
            when (type) {
                "reg_by_color" -> RegByColor(arg)
                "spot_by_color" -> SpotByColor(arg)
                "spot_by_reg" -> SpotByReg(arg)
                "park" -> Park(arg.asCar)
                "leave" -> Leave(arg.trim().toInt())
                "create" -> Create(arg.trim().toInt())
                else -> throw error("wrong command")
            }
        }
    }

data class Parking(
    private val map: Map<Int, Car> = hashMapOf(),
    val capacity: Int,
) : Map<Int, Car> by map {
    val hasSpace get() = map.size < capacity
    val firstOpen get() = (1..capacity).find { !containsKey(it) }!!
    operator fun Parking.minus(key: Int) =
        Parking(map - key, capacity)

    operator fun Parking.plus(value: Car) = firstOpen.let {
        Parking(map + (it to value), capacity) to it
    }
}


sealed class ParkingResult
data class SuccessParking(val res: Parking, val taken: Int) :
    ParkingResult()

object ParkingIsFull : ParkingResult()

val park: Parking.(Car) -> ParkingResult = { newCar ->
    if (this.hasSpace)
        (this + newCar).let { (parking, spot) ->
            SuccessParking(
                parking,
                spot
            )
        }
    else ParkingIsFull
}

sealed class LeaveResult
data class SuccessLeave(val parking: Parking) : LeaveResult()
object AlreadyEmpty : LeaveResult()

val leave: Parking.(Int) -> LeaveResult = { n ->
    if (containsKey(n)) SuccessLeave(this - n)
    else AlreadyEmpty
}


tailrec fun repl(parking: Parking): Unit =
    when (val cmd = readLine().orEmpty().asCommand) {
        is Leave -> when (val p = parking.leave(cmd.n)) {
            AlreadyEmpty -> {
                println("There is no car in spot ${cmd.n}.")
                repl(parking)
            }
            is SuccessLeave -> {
                println("Spot ${cmd.n} is free.")
                repl(p.parking)
            }
        }

        is Park -> when (val p = parking.park(cmd.car)) {
            ParkingIsFull -> {
                println("Sorry, the parking lot is full.")
                repl(parking)
            }
            is SuccessParking -> {
                println("${cmd.car.color} car parked in spot ${p.taken}.")
                repl(p.res)
            }
        }

        is Create -> {
            println("Created a parking lot with ${cmd.n} spots.")
            repl(Parking(capacity = cmd.n))
        }

        Exit -> Unit
        Status -> {
            if (parking.isEmpty())
                println("Parking lot is empty.")
            else
                parking
                    .map { it.key to it.value }.sortedBy { it.first }
                    .map { (spot, car) -> "$spot ${car.reg} ${car.color}" }
                    .forEach(::println)
            repl(parking)
        }

        is RegByColor -> {
            val res = parking
                .map { it.key to it.value }.sortedBy { it.first }
                .filter { (_, car) ->
                    car.color.lowercase() == cmd.color.lowercase()
                }.joinToString { (_, car) -> car.reg }

            println(res.ifBlank {
                "No cars with color ${cmd.color} were found."
            })
            repl(parking)
        }

        is SpotByColor -> {
            val res = parking
                .map { it.key to it.value }.sortedBy { it.first }
                .filter { (_, car) ->
                    car.color.lowercase() == cmd.color.lowercase()
                }.joinToString { (spot, _) -> spot.toString() }

            println(res.ifBlank {
                "No cars with color ${cmd.color} were found."
            })
            repl(parking)
        }

        is SpotByReg -> {
            val res = parking
                .map { it.key to it.value }.sortedBy { it.first }
                .filter { (_, car) ->
                    car.reg.lowercase() == cmd.reg.lowercase()
                }.joinToString { (spot, _) -> spot.toString() }

            println(res.ifBlank {
                "No cars with registration number ${cmd.reg} were" +
                        " found."
            })
            repl(parking)
        }
    }

val createRegex = "create \\d+".toRegex()

fun create() {
    readLine().orEmpty().let {
        when {
            createRegex.matches(it) -> {
                val n = it.splitAt(' ')[1].toInt()
                println("Created a parking lot with $n spots.")
                repl(Parking(hashMapOf(), n))
            }
            it.contentEquals("exit") -> Unit
            else -> {
                println("Sorry, a parking lot has not been created.")
                create()
            }
        }
    }
}

fun main() = create()

package com.kairos.ast.servicios.Api

data class DirectionsResponse(
    val routes: List<Route>
)

data class Route(
    val overview_polyline: Polyline,
    val legs: List<Leg>
)

data class Leg(
    val distance: Distance,
    val duration: Duration
)

data class Distance(
    val text: String,
    val value: Int
)

data class Duration(
    val text: String,
    val value: Int
)

data class Polyline(
    val points: String
)

package com.separ.alert

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.content.Intent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Button
import android.widget.TextView
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

class IranMapActivity : Activity() {
    private lateinit var mapView: MapView

    private data class City(val name: String, val lat: Double, val lon: Double)
    private val cities = listOf(
        City("تهران", 35.6892, 51.3890),
        City("تبریز", 38.0962, 46.2738),
        City("ارومیه", 37.5527, 45.0761),
        City("رشت", 37.2808, 49.5832),
        City("مشهد", 36.2605, 59.6168),
        City("اصفهان", 32.6546, 51.6680),
        City("اهواز", 31.3183, 48.6706),
        City("شیراز", 29.5918, 52.5837),
        City("بندرعباس", 27.1865, 56.2808),
        City("کرمان", 30.2839, 57.0834),
        City("زاهدان", 29.4963, 60.8629),
        City("سنندج", 35.3219, 46.9862)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = LinearLayout.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.rgb(246, 248, 251))
        }
        root.addView(TextView(this).apply {
            text = "نقشهٔ ایران و شهرهای اصلی"
            textSize = 19f
            setTextColor(Color.rgb(18, 43, 67))
            gravity = Gravity.CENTER_VERTICAL
            setPadding(18, 12, 18, 12)
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        mapView = MapView(this)
        root.addView(mapView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))
        root.addView(Button(this).apply {
            text = "نمایش لایه‌های محدودیت و NOTAM (مرجع ثانویه)"
            isAllCaps = false
            setOnClickListener {
                startActivity(Intent(this@IranMapActivity, AirspaceReferenceActivity::class.java))
            }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        root.addView(TextView(this).apply {
            text = "نقاط شهرها برای جهت‌یابی‌اند؛ نقشهٔ پایه از OpenFreeMap / OpenStreetMap است. مرز رسمی FIR و مناطق NOTAM هنوز به‌صورت خودکار روی این نقشه متصل نشده‌اند. لایهٔ NOTAM در صفحهٔ مرجع ثانویه، حکم تأیید رسمی ندارد."
            textSize = 12f
            setTextColor(Color.DKGRAY)
            setPadding(14, 10, 14, 10)
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        setContentView(root)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(32.2, 53.4))
                .zoom(4.0)
                .build()
            map.setStyle("https://tiles.openfreemap.org/styles/liberty") { style ->
                val features = cities.map { city ->
                    Feature.fromGeometry(Point.fromLngLat(city.lon, city.lat)).apply {
                        addStringProperty("name", city.name)
                    }
                }
                style.addSource(GeoJsonSource(
                    "separ-iran-cities",
                    FeatureCollection.fromFeatures(features)
                ))
                style.addLayer(CircleLayer(
                    "separ-iran-city-dots", "separ-iran-cities"
                ).withProperties(
                    circleColor("#c62828"),
                    circleRadius(5.5f),
                    circleStrokeColor("#ffffff"),
                    circleStrokeWidth(1.6f)
                ))
                style.addLayer(SymbolLayer(
                    "separ-iran-city-labels", "separ-iran-cities"
                ).withProperties(
                    textField("{name}"),
                    textSize(11f),
                    textColor("#142d4e"),
                    textHaloColor("#ffffff"),
                    textHaloWidth(1.5f),
                    textAllowOverlap(false)
                ))
            }
        }
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroy() { mapView.onDestroy(); super.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}

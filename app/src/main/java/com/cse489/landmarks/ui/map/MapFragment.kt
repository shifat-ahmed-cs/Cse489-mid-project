package com.cse489.landmarks.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.cse489.landmarks.R
import com.cse489.landmarks.data.api.LandmarkApiService
import com.cse489.landmarks.data.local.LandmarkEntity
import com.cse489.landmarks.databinding.FragmentMapBinding
import com.cse489.landmarks.ui.viewmodel.LandmarkViewModel
import com.cse489.landmarks.utils.LocationUtils
import kotlinx.coroutines.launch
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.util.Locale

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LandmarkViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMapView()
        observeLandmarks()

        binding.btnCloseCard.setOnClickListener {
            binding.cardLandmarkDetails.visibility = View.GONE
        }
    }

    private fun setupMapView() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)

        // Center Map on Bangladesh (Lat: 23.685, Lon: 90.356)
        val bangladeshPoint = GeoPoint(23.6850, 90.3563)
        binding.mapView.controller.setZoom(7.5)
        binding.mapView.controller.setCenter(bangladeshPoint)
    }

    private fun observeLandmarks() {
        viewModel.activeLandmarks.observe(viewLifecycleOwner) { landmarks ->
            if (landmarks != null) {
                updateMapMarkers(landmarks)
            }
        }
    }

    private fun updateMapMarkers(landmarks: List<LandmarkEntity>) {
        binding.mapView.overlays.clear()

        if (landmarks.isEmpty()) {
            binding.mapView.invalidate()
            return
        }

        val minScore = landmarks.minOfOrNull { it.score } ?: 0.0
        val maxScore = landmarks.maxOfOrNull { it.score } ?: 1.0

        for (landmark in landmarks) {
            val marker = Marker(binding.mapView)
            marker.position = GeoPoint(landmark.lat, landmark.lon)
            marker.title = landmark.title
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            // Color gradient based on score (low -> high: Red -> Yellow -> Green)
            val markerColor = getMarkerColorForScore(landmark.score, minScore, maxScore)
            marker.icon = createCustomMarkerIcon(markerColor)

            marker.setOnMarkerClickListener { _, _ ->
                showLandmarkDetailsCard(landmark)
                true
            }

            binding.mapView.overlays.add(marker)
        }

        binding.mapView.invalidate()
    }

    private fun showLandmarkDetailsCard(landmark: LandmarkEntity) {
        binding.cardLandmarkDetails.visibility = View.VISIBLE
        binding.txtTitle.text = landmark.title
        binding.txtScore.text = String.format(Locale.getDefault(), "Score: %.2f", landmark.score)

        val distText = if (landmark.avgDistance >= 1000) {
            String.format(Locale.getDefault(), "%.2f km", landmark.avgDistance / 1000.0)
        } else {
            String.format(Locale.getDefault(), "%.2f m", landmark.avgDistance)
        }
        binding.txtStats.text = String.format(Locale.getDefault(), "Visits: %d | Avg Dist: %s", landmark.visitCount, distText)

        if (!landmark.image.isNullOrEmpty()) {
            val fullImageUrl = if (landmark.image.startsWith("http")) landmark.image else LandmarkApiService.BASE_URL + landmark.image
            Glide.with(this)
                .load(fullImageUrl)
                .placeholder(R.drawable.ic_landmarks)
                .error(R.drawable.ic_landmarks)
                .into(binding.imgLandmark)
        } else {
            binding.imgLandmark.setImageResource(R.drawable.ic_landmarks)
        }

        binding.btnVisitLandmark.setOnClickListener {
            lifecycleScope.launch {
                val loc = LocationUtils.getCurrentLocation(requireContext())
                viewModel.visitLandmark(landmark.id, landmark.title, loc.latitude, loc.longitude)
                binding.cardLandmarkDetails.visibility = View.GONE
            }
        }
    }

    private fun getMarkerColorForScore(score: Double, minScore: Double, maxScore: Double): Int {
        val range = if (maxScore > minScore) maxScore - minScore else 1.0
        val normalized = ((score - minScore) / range).coerceIn(0.0, 1.0)

        // Interpolate Red (Low) to Green (High)
        val red = (255 * (1.0 - normalized)).toInt().coerceIn(0, 255)
        val green = (255 * normalized).toInt().coerceIn(0, 255)
        return Color.rgb(red, green, 0)
    }

    private fun createCustomMarkerIcon(colorInt: Int): Drawable {
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            color = colorInt
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, paint)

        val strokePaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, strokePaint)

        return BitmapDrawable(resources, bitmap)
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

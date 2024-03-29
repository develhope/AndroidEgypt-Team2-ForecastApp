package co.develhope.meteoapp.ui.today

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import co.develhope.meteoapp.data.Data
import co.develhope.meteoapp.data.domain.DailyDataLocal
import co.develhope.meteoapp.data.domain.HourlyForecast
import co.develhope.meteoapp.databinding.FragmentTodayScreenBinding
import co.develhope.meteoapp.network.WeatherRepo
import co.develhope.meteoapp.ui.search.adapter.DataSearches
import co.develhope.meteoapp.ui.today.adapter.HourlyForecastItems
import co.develhope.meteoapp.ui.today.adapter.TodayAdapter
import co.develhope.meteoapp.ui.today.view_model.TodayScreenViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.format.DateTimeFormatter

@AndroidEntryPoint
class TodayScreenFragment : Fragment() {

    private var _binding: FragmentTodayScreenBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<TodayScreenViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentTodayScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dataSearches = Data.getSearchCity(requireContext())

        var longitude = DataSearches.ItemSearch(
            longitude = 0.0,
            latitude = 0.0,
            recentCitySearch = "",
            admin1 = ""
        ).longitude
        if (dataSearches is DataSearches.ItemSearch) {
            longitude = dataSearches.longitude
        }

        var latitude = DataSearches.ItemSearch(
            longitude = 0.0,
            latitude = 0.0,
            recentCitySearch = "",
            admin1 = ""
        ).latitude
        if (dataSearches is DataSearches.ItemSearch) {
            latitude = dataSearches.latitude
        }

        val currentDate = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-MM-d"))
        Log.d("DATE:", currentDate)

        getDaily(latitude!!, longitude!!, currentDate, currentDate)

        setupAdapter()
        collectFromStateFlow()
    }

    private fun collectFromStateFlow() {
        lifecycleScope.launch {
            viewModel.todayScreenState.collect {
                if (it.dailyDataLocal != null) {
                    (binding.todayRecyclerview.adapter as TodayAdapter).setNewList(it.dailyDataLocal.toHourlyForecastItems())
                    Log.i("NETWORK DATA", "$it")
                } else {
                    Log.e("NETWORK ERROR", "Couldn't achieve network call. (Today Screen)")
                }
                binding.todayProgress.isVisible = it.isProgressbarVisible
            }
        }
    }

    private fun setupAdapter() {
        binding.todayRecyclerview.adapter = TodayAdapter(listOf())
    }

    private fun getDaily(
        lat: Double,
        lon: Double,
        startDate: String,
        endDate: String,
    ) {
        viewModel.fetchTodayWeather(
            lat = lat,
            lon = lon,
            startDate = startDate,
            endDate = endDate
        )
    }

    //TODO: DA FINIRE

    private fun DailyDataLocal?.toHourlyForecastItems(): List<HourlyForecastItems> {

        val filteredList = this?.filter {
            it.time.isEqual(OffsetDateTime.now()) || it.time.isAfter(OffsetDateTime.now())
        }

        val newList = mutableListOf<HourlyForecastItems>()

        newList.add(
            HourlyForecastItems.Title(
                Data.getCityLocation(requireContext()),
                OffsetDateTime.now()
            )
        )

        filteredList?.forEach { hourly ->
            newList.add(
                HourlyForecastItems.Forecast(
                    HourlyForecast(
                        date = hourly.time,
                        hourlyTemp = hourly.temperature2m?.toInt() ?: 0,
                        possibleRain = hourly.rainChance ?: 0,
                        apparentTemp = hourly.apparentTemperature?.toInt() ?: 0,
                        uvIndex = hourly.uvIndex?.toInt() ?: 0,
                        humidity = hourly.humidity ?: 0,
                        windDirection = hourly.windDirection.toString(),
                        windSpeed = hourly.windSpeed?.toInt() ?: 0,
                        cloudyness = hourly.cloudCover ?: 0,
                        rain = hourly.rain?.toInt() ?: 0,
                        forecastIndex = hourly.weathercode ?: 0,
                        isDay = hourly.isDay ?: 0

                    )
                )
            )
        }
        return newList
    }
}
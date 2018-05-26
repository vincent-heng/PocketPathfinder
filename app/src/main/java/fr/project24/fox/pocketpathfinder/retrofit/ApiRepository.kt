package fr.project24.fox.pocketpathfinder.retrofit

import fr.project24.fox.pocketpathfinder.model.Room
import io.reactivex.Observable

/**
 * Repository method to access search functionality of the api service
 */
class ApiRepository(private val apiService: ApiService) {
    fun findRoom(room_code: String): Observable<List<Room>> {
        return apiService.findRoom(query = "{\"room_code\":\"$room_code\"}")
    }
}
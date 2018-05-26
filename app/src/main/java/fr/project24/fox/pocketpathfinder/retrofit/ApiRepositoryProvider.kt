package fr.project24.fox.pocketpathfinder.retrofit

object ApiRepositoryProvider {
    fun provideRepository(): ApiRepository {
        return ApiRepository(ApiService.Factory.create())
    }
}
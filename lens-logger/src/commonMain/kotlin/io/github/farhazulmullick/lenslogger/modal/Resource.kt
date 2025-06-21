package io.github.farhazulmullick.lenslogger.modal

sealed class Resource<T> {

    /**
     * Operation is currently loading.
     */
    class Loading<Domain> : Resource<Domain>()

    /**
     * Successful operation with data.
     */
    class Success<Domain>(val data: Domain) : Resource<Domain>()

    /**
     * Operation resulted in an error.
     */
    data class Failed<Domain>(
        val data: Domain? = null,
        val throwable: Throwable? = null
    ) : Resource<Domain>()
}
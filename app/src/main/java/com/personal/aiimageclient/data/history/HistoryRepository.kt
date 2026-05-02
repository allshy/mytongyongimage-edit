package com.personal.aiimageclient.data.history

class HistoryRepository(private val dao: HistoryDao) {
    val history = dao.observeAll()

    suspend fun save(entity: HistoryEntity) = dao.insert(entity)

    suspend fun clear() = dao.clear()
}


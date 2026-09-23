package com.englishlearning.app

import android.app.Application
import com.englishlearning.app.data.seeder.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class anotada com @HiltAndroidApp para acionar a geração de código do Hilt
 * e servir como container de injeção de dependência no nível da aplicação.
 *
 * Na inicialização, dispara de forma assíncrona o [DatabaseSeeder.seedIfNeeded] para
 * popular o banco de dados Room com conteúdo JSON dos assets na primeira execução.
 *
 * Registrada no AndroidManifest.xml via android:name=".EnglishLearningApp".
 *
 * Requisito 9.2: seeding do banco executado na inicialização da aplicação.
 */
@HiltAndroidApp
class EnglishLearningApp : Application() {

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    /** Escopo de coroutine vinculado ao ciclo de vida da Application. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        seedDatabaseAsync()
    }

    /**
     * Dispara [DatabaseSeeder.seedIfNeeded] em uma coroutine de IO.
     * O [SupervisorJob] garante que falhas de seeding não derrubem outras coroutines
     * do escopo da aplicação.
     */
    private fun seedDatabaseAsync() {
        applicationScope.launch {
            databaseSeeder.seedIfNeeded()
        }
    }
}

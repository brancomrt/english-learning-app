package com.englishlearning.app.data.database.converter

import androidx.room.TypeConverter
import com.englishlearning.app.domain.model.QuestionType
import com.englishlearning.app.domain.model.ValidationStatus
import java.time.LocalDate

/**
 * Room TypeConverters para os enums [QuestionType] e [ValidationStatus],
 * e para [LocalDate] (usado nas entidades SRS).
 *
 * Room não pode persistir tipos customizados diretamente. Estes conversores
 * mapeiam cada enum para sua representação String (nome do valor) e as datas
 * para o formato ISO 8601 "yyyy-MM-dd", garantindo que migrações futuras com
 * novos valores do enum não corrompam dados existentes, e que a comparação
 * lexicográfica de datas String funcione corretamente nas queries Room.
 *
 * Requisito 9.1: persistência de todas as entidades no Room.
 * Requisito 16.2: armazenamento de dueDate e lastReviewDate como String ISO.
 */
class Converters {

    // ─── QuestionType ─────────────────────────────────────────────────────────

    /**
     * Converte [QuestionType] para String ao gravar no banco.
     * Retorna null se o valor for null (Room aceita colunas anuláveis).
     */
    @TypeConverter
    fun fromQuestionType(value: QuestionType?): String? = value?.name

    /**
     * Converte String para [QuestionType] ao ler do banco.
     * Retorna null se a String for null ou não corresponder a nenhum valor do enum,
     * evitando crashes com dados legados ou corruptos.
     */
    @TypeConverter
    fun toQuestionType(value: String?): QuestionType? =
        value?.let {
            try {
                QuestionType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

    // ─── ValidationStatus ─────────────────────────────────────────────────────

    /**
     * Converte [ValidationStatus] para String ao gravar no banco.
     */
    @TypeConverter
    fun fromValidationStatus(value: ValidationStatus?): String? = value?.name

    /**
     * Converte String para [ValidationStatus] ao ler do banco.
     * Retorna null se a String for null ou não corresponder a nenhum valor do enum.
     */
    @TypeConverter
    fun toValidationStatus(value: String?): ValidationStatus? =
        value?.let {
            try {
                ValidationStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

    // ─── LocalDate ────────────────────────────────────────────────────────────

    /**
     * Converte [LocalDate] para String ISO "yyyy-MM-dd" ao gravar no banco.
     *
     * O formato ISO garante que a comparação lexicográfica de Strings nas queries
     * Room (ex: `due_date <= :today`) funcione corretamente como comparação de datas.
     *
     * Retorna null se a data for null.
     *
     * Requisito 16.2: dueDate e lastReviewDate armazenados como String ISO.
     */
    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    /**
     * Converte String ISO "yyyy-MM-dd" para [LocalDate] ao ler do banco.
     * Retorna null se a String for null ou não puder ser parseada.
     */
    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? =
        value?.let {
            try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                null
            }
        }

    // ─── List<String> ─────────────────────────────────────────────────────────

    /** Converte List<String> para String delimitada por "|||" ao gravar no banco. */
    @TypeConverter
    fun fromStringList(value: List<String>?): String =
        value?.joinToString(separator = "|||") ?: ""

    /** Converte String delimitada por "|||" para List<String> ao ler do banco. */
    @TypeConverter
    fun toStringList(value: String?): List<String> =
        if (value.isNullOrBlank()) emptyList()
        else value.split("|||").filter { it.isNotBlank() }
}

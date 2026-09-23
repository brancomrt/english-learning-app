package com.englishlearning.app.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.englishlearning.app.data.database.entity.SrsCardEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operações de leitura e escrita de SRS cards.
 *
 * As queries de cards devidos comparam [dueDate] (armazenado como String "yyyy-MM-dd")
 * com a data atual passada como parâmetro. O formato ISO 8601 garante que a comparação
 * lexicográfica de Strings funcione corretamente como comparação de datas.
 *
 * Requisito 16.6: contagem de SRS cards devidos exibida como badge na HomeScreen.
 * Requisito 16.7: SrsReviewScreen apresenta apenas cards com dueDate <= hoje.
 * Requisito 18.6: persistência completa no Room Database.
 */
@Dao
interface SrsDao {

    /**
     * Retorna todos os SRS cards de um usuário com [dueDate] <= [today],
     * ordenados por data de vencimento (mais antigos primeiro).
     *
     * [today] deve ser passado no formato "yyyy-MM-dd" para a comparação
     * lexicográfica funcionar corretamente com as datas armazenadas.
     *
     * Requisito 16.7: exibir apenas cards devidos na SrsReviewScreen.
     */
    @Query("""
        SELECT * FROM srs_cards
        WHERE user_id = :userId AND due_date <= :today
        ORDER BY due_date ASC
    """)
    suspend fun getDueCards(userId: Long, today: String): List<SrsCardEntity>

    /**
     * Retorna a contagem de SRS cards devidos de um usuário em uma única query eficiente.
     * Utilizado pelo HomeViewModel para exibir o badge de revisão.
     *
     * [today] deve ser passado no formato "yyyy-MM-dd".
     *
     * Requisito 16.6: badge com contagem de cards devidos na HomeScreen.
     */
    @Query("""
        SELECT COUNT(*) FROM srs_cards
        WHERE user_id = :userId AND due_date <= :today
    """)
    suspend fun getDueCardCount(userId: Long, today: String): Int

    /**
     * Observa reativamente a contagem de SRS cards devidos do usuário.
     * Emite um novo valor sempre que algum card for inserido, atualizado ou
     * a data de vencimento de um card cruzar o limiar de hoje.
     *
     * Nota: como Room não observa mudanças baseadas no tempo, o Flow atualiza
     * quando há escrita no banco. O ViewModel deve recarregar ao retornar ao primeiro plano.
     *
     * [today] deve ser passado no formato "yyyy-MM-dd".
     *
     * Requisito 16.6: observable para badge de revisão na HomeScreen.
     */
    @Query("""
        SELECT COUNT(*) FROM srs_cards
        WHERE user_id = :userId AND due_date <= :today
    """)
    fun observeDueCount(userId: Long, today: String): Flow<Int>

    /**
     * Insere ou atualiza um SRS card.
     * Usado tanto na criação inicial (após primeiro exercício concluído) quanto
     * na atualização após cada revisão com o resultado do FsrsScheduler.
     *
     * Requisito 18.6: persistência de todos os parâmetros FSRS após revisão.
     */
    @Upsert
    suspend fun upsertCard(card: SrsCardEntity)

    /**
     * Retorna um SRS card específico pelo usuário, tipo e referência, ou null se
     * ainda não existir. Usado por SrsRepository.getOrCreateCard.
     *
     * Requisito 18.1: criação automática de SrsCard após resposta CORRECT/TOO_CLOSE.
     */
    @Query("""
        SELECT * FROM srs_cards
        WHERE user_id = :userId AND card_type = :cardType AND ref_id = :refId
        LIMIT 1
    """)
    suspend fun getCardByRef(userId: Long, cardType: String, refId: Long): SrsCardEntity?

    /**
     * Retorna um SRS card pelo seu ID primário, ou null se não encontrado.
     * Usado para recarregar o card atualizado após um upsert.
     */
    @Query("SELECT * FROM srs_cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: Long): SrsCardEntity?
}

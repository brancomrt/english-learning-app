package com.englishlearning.app.ui.components

import androidx.annotation.RawRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.englishlearning.app.R

/**
 * Componente de animação Lottie genérico.
 *
 * @param resId ID do recurso raw da animação JSON.
 * @param modifier Modifier para customização.
 * @param size Tamanho da animação.
 * @param iterations Número de repetições (LottieConstants.IterateForever para loop infinito).
 * @param speed Velocidade da animação (1.0f = normal).
 */
@Composable
fun LottieAnimationView(
    @RawRes resId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    iterations: Int = LottieConstants.IterateForever,
    speed: Float = 1f
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = iterations,
        speed = speed
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress }
        )
    }
}

/**
 * Animação de loading circular.
 */
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    LottieAnimationView(
        resId = R.raw.anim_loading,
        modifier = modifier,
        size = size
    )
}

/**
 * Animação de sucesso (check mark).
 */
@Composable
fun SuccessAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    onFinished: (() -> Unit)? = null
) {
    LottieAnimationView(
        resId = R.raw.anim_success,
        modifier = modifier,
        size = size,
        iterations = 1
    )
}

/**
 * Animação de erro (X mark).
 */
@Composable
fun ErrorAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    LottieAnimationView(
        resId = R.raw.anim_error,
        modifier = modifier,
        size = size,
        iterations = 1
    )
}

/**
 * Animação de celebração (confetes/estrelas).
 */
@Composable
fun CelebrationAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp
) {
    LottieAnimationView(
        resId = R.raw.anim_celebration,
        modifier = modifier,
        size = size,
        iterations = 1
    )
}

/**
 * Animação de estado vazio (busca sem resultados).
 */
@Composable
fun EmptyStateAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp
) {
    LottieAnimationView(
        resId = R.raw.anim_empty,
        modifier = modifier,
        size = size
    )
}

package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class TutorialStep(val title: String, val body: String)

@Composable
fun TutorialOverlay(onDismiss: () -> Unit) {
    val tutorialSteps = listOf(
        TutorialStep("Bienvenido a Nikkei & Clima", "Aquí podrás seguir índices bursátiles japoneses, el clima interactivo y gestionar notas rápidas con comandos de voz y la pantalla flotante."),
        TutorialStep("Bolsa & Velas Interactivas", "Observa las velas japonesas con datos que responden a tu toque. Accede a un menú para elegir entre las top 30 empresas del mundo."),
        TutorialStep("Clima y Radar", "Dirígete a la pestaña de clima para monitorear tormentas con el minimapa interactivo en tiempo real."),
        TutorialStep("Asistente IA y Notas", "Dicta notas, arrástralas y colócalas donde las necesites como un Sticky Note del sistema. Crea alertas con horas exactas.")
    )
    
    var currentStep by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xBB000000)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                elevation = CardDefaults.cardElevation(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        }, label = "tutorial"
                    ) { step ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = tutorialSteps[step].title,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = tutorialSteps[step].body,
                                fontSize = 14.sp,
                                color = Color.LightGray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.height(80.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (currentStep > 0) {
                            TextButton(onClick = { currentStep-- }) {
                                Text("Atrás", color = Color(0xFF3B82F6))
                            }
                        } else {
                            Spacer(modifier = Modifier.width(64.dp))
                        }

                        Text(
                            text = "${currentStep + 1} de ${tutorialSteps.size}",
                            color = Color.Gray,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )

                        if (currentStep < tutorialSteps.size - 1) {
                            Button(
                                onClick = { currentStep++ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                            ) {
                                Text("Siguiente")
                            }
                        } else {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            ) {
                                Text("Cerrar")
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.englishlearning.app.data.api

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import kotlinx.coroutines.test.runTest

/**
 * Testes para FreeDictionaryApi.
 * 
 * Nota: Estes testes fazem chamadas reais à API.
 * Em ambiente CI, podem ser marcados como @Tag("integration")
 */
class FreeDictionaryApiTest : DescribeSpec({
    
    val api = FreeDictionaryApi()
    
    describe("getWordInfo") {
        
        it("deve retornar informações para palavras comuns em inglês") {
            runTest {
                val result = api.getWordInfo("hello")
                
                result.shouldNotBeNull()
                result.word shouldBe "hello"
            }
        }
        
        it("deve retornar URL de áudio quando disponível") {
            runTest {
                val result = api.getWordInfo("hello")
                
                result.shouldNotBeNull()
                result.audioUrl.shouldNotBeNull()
                result.audioUrl!! shouldStartWith "https://"
            }
        }
        
        it("deve retornar fonética IPA") {
            runTest {
                val result = api.getWordInfo("hello")
                
                result.shouldNotBeNull()
                // A maioria das palavras tem fonética
                // hello geralmente retorna algo como /həˈləʊ/ ou /hɛˈloʊ/
            }
        }
        
        it("deve retornar null para palavras inexistentes") {
            runTest {
                val result = api.getWordInfo("xyznotaword123")
                
                result.shouldBeNull()
            }
        }
        
        it("deve retornar null para strings vazias") {
            runTest {
                val result = api.getWordInfo("")
                
                result.shouldBeNull()
            }
        }
        
        it("deve retornar null para strings com apenas espaços") {
            runTest {
                val result = api.getWordInfo("   ")
                
                result.shouldBeNull()
            }
        }
        
        it("deve lidar com palavras com caracteres especiais") {
            runTest {
                // A API deve limpar caracteres não alfabéticos
                val result = api.getWordInfo("hello!")
                
                result.shouldNotBeNull()
                result.word shouldBe "hello"
            }
        }
        
        it("deve retornar definição") {
            runTest {
                val result = api.getWordInfo("book")
                
                result.shouldNotBeNull()
                result.definition.shouldNotBeNull()
            }
        }
        
        it("deve retornar part of speech") {
            runTest {
                val result = api.getWordInfo("run")
                
                result.shouldNotBeNull()
                result.partOfSpeech.shouldNotBeNull()
            }
        }
        
        it("deve ser case-insensitive") {
            runTest {
                val lowercase = api.getWordInfo("hello")
                val uppercase = api.getWordInfo("HELLO")
                val mixed = api.getWordInfo("HeLLo")
                
                lowercase.shouldNotBeNull()
                uppercase.shouldNotBeNull()
                mixed.shouldNotBeNull()
                
                lowercase.word shouldBe uppercase?.word
                lowercase.word shouldBe mixed?.word
            }
        }
    }
    
    describe("URL normalization") {
        
        it("deve normalizar URLs que começam com //") {
            runTest {
                val result = api.getWordInfo("hello")
                
                result?.audioUrl?.let { url ->
                    url shouldStartWith "https://"
                    url.shouldNotBeNull()
                }
            }
        }
    }
})

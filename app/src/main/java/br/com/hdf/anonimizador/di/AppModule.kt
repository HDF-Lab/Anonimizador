package br.com.hdf.anonimizador.di

import br.com.hdf.anonimizador.data.detector.BrazilRegexPiiDetector
import br.com.hdf.anonimizador.data.ingestion.AndroidDocumentExtractor
import br.com.hdf.anonimizador.data.masking.SafeTextMasker
import br.com.hdf.anonimizador.domain.detector.DefaultSpanConsolidator
import br.com.hdf.anonimizador.domain.detector.PiiDetector
import br.com.hdf.anonimizador.domain.detector.SpanConsolidator
import br.com.hdf.anonimizador.domain.ingestion.DocumentExtractor
import br.com.hdf.anonimizador.domain.masking.TextMasker
import br.com.hdf.anonimizador.data.parser.AndroidWhatsAppParser
import br.com.hdf.anonimizador.domain.parser.WhatsAppParser
import br.com.hdf.anonimizador.domain.usecase.AnonymizeContentUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {

    @Binds
    @Singleton
    abstract fun bindDocumentExtractor(impl: AndroidDocumentExtractor): DocumentExtractor

    @Binds
    @Singleton
    abstract fun bindTextMasker(impl: SafeTextMasker): TextMasker

    @Binds
    @IntoSet
    abstract fun bindBrazilRegexDetector(impl: BrazilRegexPiiDetector): PiiDetector
}

@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideWhatsAppParser(): WhatsAppParser = AndroidWhatsAppParser()

    @Provides
    @Singleton
    fun provideSpanConsolidator(): SpanConsolidator = DefaultSpanConsolidator()

    @Provides
    @Singleton
    fun provideAnonymizeContentUseCase(
        whatsAppParser: WhatsAppParser,
        detectors: Set<@JvmSuppressWildcards PiiDetector>,
        spanConsolidator: SpanConsolidator,
        textMasker: TextMasker
    ): AnonymizeContentUseCase = AnonymizeContentUseCase(
        whatsAppParser = whatsAppParser,
        detectors = detectors,
        spanConsolidator = spanConsolidator,
        textMasker = textMasker
    )
}

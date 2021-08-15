package br.com.zup.edu

import com.google.protobuf.Any
import com.google.rpc.Code
import io.grpc.Status
import io.grpc.protobuf.StatusProto
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FretesGrpcServer : FretesServiceGrpc.FretesServiceImplBase() {


    private val logger = LoggerFactory.getLogger(FretesGrpcServer::class.java)

    override fun calculaFrete(
        request: CalculaFreteRequest?,
        responseObserver: StreamObserver<CalculaFreteResponse>?
    ) {
        logger.info("Calculando frete para requeste: $request")

        val cep = request?.cep

        validateRequest(
            cep == null || cep.isBlank(),
            "CEP deve ser informado",
            Status.INVALID_ARGUMENT,
            null,
            responseObserver
        )

        validateRequest(
            !cep!!.matches("[0-9]{5}-[0-9]{3}".toRegex()),
            "CEP deve estar em um formato válido",
            Status.INVALID_ARGUMENT,
            "formato esperado deve ser 99999-999",
            responseObserver
        )

        if (cep.endsWith("333")) {

            val statusProto = com.google.rpc.Status
                .newBuilder()
                .setCode(Code.PERMISSION_DENIED_VALUE)
                .setMessage("usuario não pode acessar esse recurso")
                .addDetails(Any.pack(
                    ErrorDetails
                        .newBuilder()
                        .setCode(401)
                        .setMessage("token expirado")
                        .build()))
                .build()

            val e = StatusProto.toStatusRuntimeException(statusProto)
            responseObserver?.onError(e)
        }

        var valor = 0.0
        try {
            valor = Random.nextDouble(from = 0.0, until = 140.0)
            if (valor > 100.0)
                throw IllegalArgumentException("Erro inesperado ao executar logica de negócio!")
        } catch (e: Exception) {
            responseObserver?.onError(
                Status.INTERNAL
                    .withDescription(e.message)
                    .withCause(e)
                    .asRuntimeException()
            )
        }

        val response = CalculaFreteResponse
            .newBuilder()
            .setCep(request.cep)
            .setValor(valor)
            .build()

        logger.info("Frete calculado: $response")

        responseObserver!!.onNext(response)
        responseObserver.onCompleted()
    }

    private fun validateRequest(
        condition: Boolean,
        description: String,
        status: Status,
        augmentDescription: String?,
        responseObserver: StreamObserver<CalculaFreteResponse>?
    ) {
        if (condition) {
            val error = status
                .withDescription(description)
                .augmentDescription(augmentDescription)
                .asRuntimeException()
            responseObserver?.onError(error)
        }
    }
}
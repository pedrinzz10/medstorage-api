package com.saas.MedStorage_api.inventory.scheduler;

import com.saas.MedStorage_api.inventory.service.StockAlertService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StockAlertScheduler {

    private final StockAlertService stockAlertService;
    private final boolean enabled;

    public StockAlertScheduler(
            StockAlertService stockAlertService,
            @Value("${app.stock.low-alert-enabled:true}") boolean enabled) {
        this.stockAlertService = stockAlertService;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${app.stock.low-alert-cron:0 0 8 * * *}")
    public void run() {
        if (!enabled) {
            log.debug("Alerta de estoque baixo desabilitado via configuração");
            return;
        }
        // checkAndNotify tambem cobre lotes proximos do vencimento; o retorno
        // reflete apenas a contagem de produtos criticos, entao logamos de
        // forma incondicional em vez de inferir se um email foi enviado.
        int criticos = stockAlertService.checkAndNotify();
        log.debug("Verificação de alerta de estoque concluída ({} produto(s) crítico(s))", criticos);
    }
}

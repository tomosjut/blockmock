package nl.blockmock.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "trigger_config_cron")
@DiscriminatorValue("CRON")
@Getter
@Setter
public class CronTriggerConfig extends TriggerConfig {

    @Column(name = "cron_expression", length = 100)
    private String cronExpression;
}

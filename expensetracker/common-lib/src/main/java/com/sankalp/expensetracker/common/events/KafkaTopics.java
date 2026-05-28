package com.sankalp.expensetracker.common.events;

public final class KafkaTopics {
    private KafkaTopics() {}
    public static final String USER_REGISTERED = "user.registered";
    public static final String USER_INVITED = "user.invited";
    public static final String GROUP_CREATED = "group.created";
    public static final String EXPENSE_CREATED = "expense.created";
    public static final String EXPENSE_UPDATED = "expense.updated";
    public static final String SETTLEMENT_COMPLETED = "settlement.completed";
}

package com.khimkhaosow.craftmastery.gui;

/**
 * Перечисление возможных состояний узла рецепта
 */
public enum NodeState {
    /** Рецепт изучен и доступен для крафта */
    STUDIED,
    
    /** Рецепт доступен для изучения */
    AVAILABLE,
    
    /** Рецепт заблокирован */
    LOCKED
}
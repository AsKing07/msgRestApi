package com.bschool.msgrestapi.security;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injecte l'id de l'utilisateur connecté (JWT → SecurityContext).
 * Ne pas confondre avec un paramètre HTTP : la valeur vient du token, pas de la requête.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Parameter(hidden = true)
public @interface CurrentUserId {
}

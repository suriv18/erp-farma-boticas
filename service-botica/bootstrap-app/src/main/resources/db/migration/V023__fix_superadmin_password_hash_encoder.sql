UPDATE sch_seguridad.credencial_local c
   SET password_hash = '{pbkdf2@SpringSecurity_v5_8}5a7a90bae394c58e5e84d5b05807b776fd314b974c4256bbfa59e7ce6bde48ca909dedc4bf5858d142d9a0be956e76ba',
       updated_at = CURRENT_TIMESTAMP
FROM sch_seguridad.membership m
JOIN sch_seguridad.identidad i ON i.id = m.identidad_id
WHERE c.membership_id = m.id
  AND i.email = 'wilton.sullcaray.r@gmail.com';

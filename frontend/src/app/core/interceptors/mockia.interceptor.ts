import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { of, delay } from 'rxjs';

export const mockIaInterceptor: HttpInterceptorFn = (req, next) => {
  // Si l'URL contient 'api/ai', on simule la réponse
  if (req.url.includes('/api/conversions')) {
    console.log('Interception de la requête IA (Mock)');
    return of(new HttpResponse({ 
      status: 200, 
      body: { result: "Résultat simulé" } 
    })).pipe(delay(1000));
  }
  
  // Sinon, on laisse passer la requête normalement
  return next(req);
};
import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, startWith, tap, shareReplay } from 'rxjs/operators';
import { ConversionService } from './conversion.service';
import { UsageStatsDto } from '../models/conversion.model';

/**
 * Usage Stats Cache Service
 * 
 * Debounces and caches usage stats to reduce API spam
 * 
 * Features:
 * - Debounces refresh requests (default 2s)
 * - Caches stats in memory
 * - Shares single subscription
 * - Manual refresh available
 * 
 * Why debounce?
 * Without debounce, calling loadUsageStats() multiple times quickly
 * would fire multiple HTTP requests. With debounce, rapid calls
 * are coalesced into a single request after 2s of inactivity.
 * 
 * Example:
 *   loadUsageStats() at 0ms
 *   loadUsageStats() at 50ms   ← Ignored, waits
 *   loadUsageStats() at 100ms  ← Ignored, timer resets
 *   ... 2s passes ...
 *   Single HTTP request fires at 2100ms
 */
@Injectable({
  providedIn: 'root'
})
export class UsageStatsCacheService {

  private readonly DEBOUNCE_TIME_MS = 2000; // Wait 2s before executing refresh
  private readonly CACHE_TTL_MS = 60000; // Cache for 1 minute

  private refreshTrigger$ = new Subject<void>();
  private cachedStats: UsageStatsDto | null = null;
  private lastFetchTime = 0;

  // Shared observable (replayed to new subscribers)
  public usageStats$: Observable<UsageStatsDto>;

  constructor(private conversionService: ConversionService) {
    // Setup debounced refresh pipeline
    this.usageStats$ = this.refreshTrigger$.pipe(
      // 1. Debounce to prevent spam (wait 2s before executing)
      debounceTime(this.DEBOUNCE_TIME_MS),
      
      // 2. Check if cache is still valid
      switchMap(() => this.fetchOrCache()),
      
      // 3. Cache the result in memory
      tap(stats => {
        this.cachedStats = stats;
        this.lastFetchTime = Date.now();
      }),
      
      // 4. Share subscription (all subscribers get same response)
      shareReplay({ 
        bufferSize: 1,
        refCount: true 
      })
    );
  }

  /**
   * Request usage stats refresh (debounced)
   * Can be called multiple times rapidly - only one HTTP request will fire
   */
  public refreshUsageStats(): void {
    this.refreshTrigger$.next();
  }

  /**
   * Get current cached stats (synchronous)
   * Returns null if not loaded yet
   */
  public getCachedStats(): UsageStatsDto | null {
    return this.cachedStats;
  }

  /**
   * Check if cache is still fresh
   */
  private isCacheFresh(): boolean {
    if (!this.cachedStats) return false;
    const timeSinceFetch = Date.now() - this.lastFetchTime;
    return timeSinceFetch < this.CACHE_TTL_MS;
  }

  /**
   * Fetch from backend or return cached version
   */
  private fetchOrCache(): Observable<UsageStatsDto> {
    // If cache is fresh, return cached value wrapped in observable
    if (this.isCacheFresh() && this.cachedStats) {
      console.log('[UsageStatsCache] Returning cached stats (TTL: 60s)');
      return new Observable(observer => {
        observer.next(this.cachedStats!);
        observer.complete();
      });
    }

    // Cache miss or expired - fetch from backend
    console.log('[UsageStatsCache] Cache miss/expired - fetching from backend');
    return this.conversionService.getUserUsageStats();
  }

  /**
   * Force cache invalidation (used after successful conversion)
   */
  public invalidateCache(): void {
    this.cachedStats = null;
    this.lastFetchTime = 0;
  }
}

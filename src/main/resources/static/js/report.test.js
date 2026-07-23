/**
 * Unit tests for Spring Test Profiler report functionality
 * These tests focus on individual methods without full initialization
 */

// Import the module
const reportModule = require('./report.js');
const { toggleClass, toggleTheorySection, ContextComparator, initializeReport } = reportModule;

describe('Report Unit Tests', () => {
  beforeEach(() => {
    // Reset DOM
    document.body.innerHTML = '';
    jest.clearAllMocks();
  });

  describe('toggleClass', () => {
    test('should toggle show class on next sibling element', () => {
      document.body.innerHTML = `
        <div id="test-element"></div>
        <div id="next-element" class="methods"></div>
      `;

      const element = document.getElementById('test-element');
      const nextElement = document.getElementById('next-element');

      toggleClass(element);
      expect(nextElement.classList.contains('show')).toBe(true);

      toggleClass(element);
      expect(nextElement.classList.contains('show')).toBe(false);
    });

    test('should handle missing next sibling element gracefully', () => {
      document.body.innerHTML = '<div id="test-element"></div>';
      const element = document.getElementById('test-element');

      expect(() => toggleClass(element)).not.toThrow();
    });
  });

  describe('toggleTheorySection', () => {
    beforeEach(() => {
      document.body.innerHTML = `
        <div id="theory-content" style="display: block;"></div>
        <span id="theory-toggle-icon">▼</span>
      `;
    });

    test('should hide theory section when visible', () => {
      toggleTheorySection();

      const content = document.getElementById('theory-content');
      const icon = document.getElementById('theory-toggle-icon');

      expect(content.style.display).toBe('none');
      expect(icon.textContent).toBe('▶');
      expect(icon.classList.contains('expanded')).toBe(false);
    });

    test('should show theory section when hidden', () => {
      const content = document.getElementById('theory-content');
      content.style.display = 'none';

      toggleTheorySection();

      expect(content.style.display).toBe('block');
      expect(document.getElementById('theory-toggle-icon').textContent).toBe('▼');
      expect(document.getElementById('theory-toggle-icon').classList.contains('expanded')).toBe(true);
    });

    test('should handle missing elements gracefully', () => {
      document.body.innerHTML = '';
      expect(() => toggleTheorySection()).not.toThrow();
    });
  });

  describe('ContextComparator - Utility Methods', () => {
    let comparator;
    let mockContextData;

    beforeEach(() => {
      mockContextData = [
        {
          contextKey: 'context-1',
          numberOfBeans: 50,
          testClasses: ['com.example.Test1', 'com.example.Test2'],
          contextConfiguration: {
            locations: ['classpath:test1.xml'],
            classes: ['com.example.Config1'],
            contextInitializerClasses: [],
            activeProfiles: ['test'],
            propertySourceLocations: [],
            propertySourceProperties: [],
            contextCustomizers: [],
            contextLoader: 'org.springframework.test.context.support.DelegatingSmartContextLoader',
            parent: null
          }
        }
      ];

      // Mock the init method to prevent full initialization
      const originalInit = ContextComparator.prototype.init;
      ContextComparator.prototype.init = jest.fn();

      window.contextStatistics = mockContextData;
      comparator = new ContextComparator();

      // Restore original init
      ContextComparator.prototype.init = originalInit;
    });

    test('arraysEqual should compare arrays correctly', () => {
      expect(comparator.arraysEqual(['a', 'b'], ['b', 'a'])).toBe(true);
      expect(comparator.arraysEqual(['a', 'b'], ['a', 'b'])).toBe(true);
      expect(comparator.arraysEqual(['a', 'b'], ['a', 'c'])).toBe(false);
      expect(comparator.arraysEqual(['a'], ['a', 'b'])).toBe(false);
    });

    test('truncateString should truncate strings correctly', () => {
      expect(comparator.truncateString('short', 10)).toBe('short');
      expect(comparator.truncateString('this is a very long string', 10)).toBe('this is a ...');
      expect(comparator.truncateString(null)).toBe(null);
      expect(comparator.truncateString(undefined)).toBe(undefined);
    });

    test('formatValueArray should format arrays correctly', () => {
      expect(comparator.formatValueArray(['a', 'b', 'c'])).toBe('a, b, c');
      expect(comparator.formatValueArray([])).toBe('None');
      expect(comparator.formatValueArray(null)).toBe('None');
      expect(comparator.formatValueArray(['very', 'long', 'array', 'with', 'many', 'items'], 20))
        .toBe('very, long, array, w...');
    });

    test('getFeatureValue should extract config values correctly', () => {
      const config = mockContextData[0].contextConfiguration;

      expect(comparator.getFeatureValue(config, 'contextLoader'))
        .toBe('org.springframework.test.context.support.DelegatingSmartContextLoader');
      expect(comparator.getFeatureValue(config, 'parent')).toBeNull();
      expect(comparator.getFeatureValue(config, 'locations')).toEqual(['classpath:test1.xml']);
      expect(comparator.getFeatureValue(config, 'nonexistent')).toEqual([]);
    });

    test('getContextFeatures should extract all features', () => {
      const features = comparator.getContextFeatures(mockContextData[0]);

      expect(features).toHaveLength(9);
      expect(features[0]).toEqual({
        name: 'Locations',
        value: ['classpath:test1.xml'],
        key: 'locations'
      });
      expect(features[4]).toEqual({
        name: 'Active Profiles',
        value: ['test'],
        key: 'activeProfiles'
      });
    });

    test('updateCompareButton should update button state correctly', () => {
      const compareBtn = { disabled: true };
      document.getElementById = jest.fn(() => compareBtn);

      // Mock console.log to avoid spam
      console.log = jest.fn();

      // Initially disabled
      comparator.updateCompareButton();
      expect(compareBtn.disabled).toBe(true);

      // Enable when different contexts are selected
      comparator.selectedContextA = { contextKey: 'context-1' };
      comparator.selectedContextB = { contextKey: 'context-2' };
      comparator.updateCompareButton();
      expect(compareBtn.disabled).toBe(false);

      // Disable when same contexts are selected
      comparator.selectedContextB = { contextKey: 'context-1' };
      comparator.updateCompareButton();
      expect(compareBtn.disabled).toBe(true);
    });
  });

  describe('initializeReport', () => {
    beforeEach(() => {
      document.body.innerHTML = `
        <script type="application/json" id="context-statistics-json">
          [{"contextKey": "test", "numberOfBeans": 10, "testClasses": []}]
        </script>
      `;

      // Mock console methods
      console.error = jest.fn();
      console.log = jest.fn();
    });

    test('should parse JSON context statistics correctly', () => {
      // Mock document.getElementById to return the script element
      const mockScriptElement = {
        textContent: '[{"contextKey": "test", "numberOfBeans": 10, "testClasses": []}]'
      };
      document.getElementById = jest.fn((id) => {
        if (id === 'context-statistics-json') return mockScriptElement;
        return null;
      });

      initializeReport();

      expect(window.contextStatistics).toHaveLength(1);
      expect(window.contextStatistics[0].contextKey).toBe('test');
    });

    test('should handle malformed JSON gracefully', () => {
      const mockScriptElement = {
        textContent: 'invalid json'
      };
      document.getElementById = jest.fn((id) => {
        if (id === 'context-statistics-json') return mockScriptElement;
        return null;
      });

      initializeReport();

      expect(console.error).toHaveBeenCalledWith('Failed to parse context statistics JSON:', expect.any(Error));
      expect(window.contextStatistics).toEqual([]);
    });

    test('should handle missing JSON script element', () => {
      document.getElementById = jest.fn(() => null);

      expect(() => initializeReport()).not.toThrow();
      expect(window.contextStatistics).toEqual([]);
    });
  });
});

describe('Context Cache Timeline', () => {
  const { timelineTickFormat, formatTimelineDuration, buildTimelineRows } = reportModule;

  describe('timelineTickFormat', () => {
    test('should use milliseconds for very short runs', () => {
      const format = timelineTickFormat(500);
      expect(format(250)).toBe('250ms');
    });

    test('should use seconds with decimals for short runs', () => {
      const format = timelineTickFormat(15000);
      expect(format(12500)).toBe('12.5s');
    });

    test('should use whole seconds for runs above 20 seconds', () => {
      const format = timelineTickFormat(90000);
      expect(format(45000)).toBe('45s');
    });

    test('should use m:ss for runs up to an hour', () => {
      const format = timelineTickFormat(20 * 60 * 1000);
      expect(format(247000)).toBe('4:07');
    });

    test('should use h:mm:ss for runs above an hour', () => {
      const format = timelineTickFormat(2 * 3600 * 1000);
      expect(format(3723000)).toBe('1:02:03');
    });
  });

  describe('formatTimelineDuration', () => {
    test('should format sub-second durations as milliseconds', () => {
      expect(formatTimelineDuration(850)).toBe('850ms');
    });

    test('should format seconds with one decimal', () => {
      expect(formatTimelineDuration(5300)).toBe('5.3s');
    });

    test('should format minutes and seconds', () => {
      expect(formatTimelineDuration(125000)).toBe('2m 05s');
    });
  });

  describe('buildTimelineRows', () => {
    const baseTimeMs = 1700000000000;

    test('should return empty rows for empty or malformed input', () => {
      expect(buildTimelineRows(null).rows).toEqual([]);
      expect(buildTimelineRows({}).rows).toEqual([]);
      expect(buildTimelineRows({ contexts: [] }).rows).toEqual([]);
      expect(buildTimelineRows({ contexts: [{ contextKey: 'context-0' }] }).rows).toEqual([]);
    });

    test('should extend open segments to the end of the test run', () => {
      const result = buildTimelineRows({
        testRunStartMs: baseTimeMs,
        testRunEndMs: baseTimeMs + 60000,
        contexts: [
          {
            contextKey: 'context-0',
            segments: [{ startMs: baseTimeMs + 5000, loadMs: 2000, removedMs: null, removalReason: null }]
          }
        ]
      });

      expect(result.rows).toHaveLength(1);
      // startMs is the cache-entry moment; the load is drawn leading up to it
      expect(result.rows[0].relLoadStartMs).toBe(3000);
      expect(result.rows[0].relStartMs).toBe(5000);
      expect(result.rows[0].relEndMs).toBe(60000);
      expect(result.rows[0].removed).toBe(false);
      expect(result.totalDurationMs).toBe(60000);
    });

    test('should keep removal time and reason for removed segments', () => {
      const result = buildTimelineRows({
        testRunStartMs: baseTimeMs,
        testRunEndMs: baseTimeMs + 60000,
        contexts: [
          {
            contextKey: 'context-0',
            segments: [
              { startMs: baseTimeMs + 1000, loadMs: 500, removedMs: baseTimeMs + 10000, removalReason: 'DIRTIES_CONTEXT' }
            ]
          }
        ]
      });

      expect(result.rows[0].relEndMs).toBe(10000);
      expect(result.rows[0].removed).toBe(true);
      expect(result.rows[0].removalReason).toBe('DIRTIES_CONTEXT');
    });

    test('should produce one labeled row per lifespan for re-created contexts', () => {
      const result = buildTimelineRows({
        testRunStartMs: baseTimeMs,
        testRunEndMs: baseTimeMs + 60000,
        contexts: [
          {
            contextKey: 'context-0',
            segments: [
              { startMs: baseTimeMs, loadMs: 500, removedMs: baseTimeMs + 10000, removalReason: 'DIRTIES_CONTEXT' },
              { startMs: baseTimeMs + 11000, loadMs: 400, removedMs: null, removalReason: null }
            ]
          }
        ]
      });

      expect(result.rows).toHaveLength(2);
      expect(result.rows[0].rowLabel).toBe('context-0');
      expect(result.rows[1].rowLabel).toBe('context-0 (2)');
    });

    test('should extend the axis when a load started before the recorded run start', () => {
      const result = buildTimelineRows({
        testRunStartMs: baseTimeMs,
        testRunEndMs: baseTimeMs + 60000,
        contexts: [
          {
            contextKey: 'context-0',
            segments: [
              { startMs: baseTimeMs + 1000, loadMs: 8000, removedMs: baseTimeMs + 3000, removalReason: 'DIRTIES_CONTEXT' }
            ]
          }
        ]
      });

      // Load started 7s before testRunStartMs, so t0 moves back to keep the full load visible
      expect(result.t0).toBe(baseTimeMs - 7000);
      expect(result.rows[0].relLoadStartMs).toBe(0);
      expect(result.rows[0].relStartMs).toBe(8000);
      expect(result.rows[0].loadMs).toBe(8000);
    });

    test('should sort rows by relative start time', () => {
      const result = buildTimelineRows({
        testRunStartMs: baseTimeMs,
        testRunEndMs: baseTimeMs + 60000,
        contexts: [
          {
            contextKey: 'context-1',
            segments: [{ startMs: baseTimeMs + 20000, loadMs: 100, removedMs: null, removalReason: null }]
          },
          {
            contextKey: 'context-0',
            segments: [{ startMs: baseTimeMs + 1000, loadMs: 100, removedMs: null, removalReason: null }]
          }
        ]
      });

      expect(result.rows.map(row => row.contextKey)).toEqual(['context-0', 'context-1']);
    });

    test('should fall back to segment bounds when run bounds are missing', () => {
      const result = buildTimelineRows({
        contexts: [
          {
            contextKey: 'context-0',
            segments: [{ startMs: baseTimeMs + 500, loadMs: 1000, removedMs: baseTimeMs + 4000, removalReason: 'CACHE_EVICTION' }]
          }
        ]
      });

      // t0 falls back to the earliest load start (cache entry minus load duration)
      expect(result.t0).toBe(baseTimeMs - 500);
      expect(result.rows[0].relLoadStartMs).toBe(0);
      expect(result.rows[0].relStartMs).toBe(1000);
      expect(result.totalDurationMs).toBe(4500);
    });
  });
});

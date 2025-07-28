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

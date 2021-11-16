import chai from 'chai'
import hello from '../handler.mjs'
const expect = chai.expect

describe('The handler should exist', () => {
  it('should return "hello"', () => {
    expect(hello()).to.equal('hello')
  })
})
